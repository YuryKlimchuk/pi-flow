package com.hydroyura.agents.piflow.workspace.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import com.hydroyura.agents.piflow.workspace.api.WorkspaceException;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceLock;
import com.hydroyura.agents.piflow.workspace.domain.Lease;
import com.hydroyura.agents.piflow.workspace.domain.LockBusyException;
import com.hydroyura.agents.piflow.workspace.domain.LockStore;
import com.hydroyura.agents.piflow.workspace.domain.WorkspaceLayout;
import com.hydroyura.agents.piflow.workspace.infrastructure.PropertiesWorkspaceLayout;
import com.hydroyura.agents.piflow.workspace.infrastructure.WorkspaceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceLockServiceTest {

	@TempDir
	Path tempDir;

	@Mock
	private LockStore lockStore;

	private WorkspaceLockService service;

	@BeforeEach
	void setUp() {
		WorkspaceLayout layout = new PropertiesWorkspaceLayout(
				new WorkspaceProperties(tempDir.resolve("ws"), tempDir.resolve("locks"), Duration.ofMinutes(1)));
		service = new WorkspaceLockService(lockStore, layout);
	}

	@Test
	void acquire_whenFree_shouldReturnLease() {
		Lease lease = new Lease("github.com-acme-repo", "WF-1", "token", Instant.now().plusSeconds(60));
		when(lockStore.acquire("github.com-acme-repo", "WF-1")).thenReturn(lease);

		WorkspaceLock lock = service.acquire("https://github.com/acme/repo.git", "WF-1");

		assertThat(lock.token()).isEqualTo("token");
		assertThat(lock.owner()).isEqualTo("WF-1");
	}

	@Test
	void acquire_whenBusy_shouldMapToLockBusy() {
		when(lockStore.acquire("github.com-acme-repo", "WF-2"))
				.thenThrow(new LockBusyException("WF-1", Instant.now().plusSeconds(60)));

		WorkspaceException error = catchThrowableOfType(
				() -> service.acquire("https://github.com/acme/repo.git", "WF-2"), WorkspaceException.class);

		assertThat(error.errorType()).isEqualTo(WorkspaceException.ErrorType.LOCK_BUSY);
	}

	@Test
	void release_shouldDelegateWithKeyAndToken() {
		WorkspaceLock lock = new WorkspaceLock("github.com-acme-repo", "WF-1", "token", Instant.now().plusSeconds(60));

		service.release(lock);

		verify(lockStore).release("github.com-acme-repo", "token");
	}
}
