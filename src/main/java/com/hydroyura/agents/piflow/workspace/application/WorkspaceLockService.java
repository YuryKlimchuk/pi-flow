package com.hydroyura.agents.piflow.workspace.application;

import com.hydroyura.agents.piflow.workspace.api.WorkspaceException;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceLock;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceLockOperations;
import com.hydroyura.agents.piflow.workspace.domain.LockBusyException;
import com.hydroyura.agents.piflow.workspace.domain.LockStore;
import com.hydroyura.agents.piflow.workspace.domain.Lease;
import com.hydroyura.agents.piflow.workspace.domain.WorkspaceLayout;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Реализует публичный порт жизненного цикла блокировки репозитория.
 */
@Service
@RequiredArgsConstructor
public class WorkspaceLockService implements WorkspaceLockOperations {

	private final LockStore lockStore;
	private final WorkspaceLayout layout;

	@Override
	public WorkspaceLock acquire(String repositoryUrl, String owner) {
		if (repositoryUrl == null || repositoryUrl.isBlank()) {
			throw WorkspaceException.validation("repositoryUrl must not be blank");
		}
		if (owner == null || owner.isBlank()) {
			throw WorkspaceException.validation("owner must not be blank");
		}
		String key = layout.repositoryKey(repositoryUrl);
		try {
			Lease lease = lockStore.acquire(key, owner);
			return new WorkspaceLock(lease.key(), lease.owner(), lease.token(), lease.expiresAt());
		} catch (LockBusyException e) {
			throw WorkspaceErrors.map(e);
		}
	}

	@Override
	public void release(WorkspaceLock lock) {
		if (lock == null) {
			return;
		}
		lockStore.release(lock.repositoryKey(), lock.token());
	}
}
