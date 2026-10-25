package com.hydroyura.agents.piflow.workspace.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.hydroyura.agents.piflow.workspace.domain.Lease;
import com.hydroyura.agents.piflow.workspace.domain.LockBusyException;
import com.hydroyura.agents.piflow.workspace.domain.WorkspaceLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileLockStoreTest {

	@TempDir
	Path tempDir;

	private Path lockDir;
	private FileLockStore store;

	@BeforeEach
	void setUp() {
		lockDir = tempDir.resolve("locks");
		WorkspaceProperties properties = new WorkspaceProperties(tempDir.resolve("ws"), lockDir, Duration.ofMinutes(1));
		WorkspaceLayout layout = new PropertiesWorkspaceLayout(properties);
		store = new FileLockStore(layout, properties);
	}

	@Test
	void acquire_whenFree_shouldCreateLease() {
		Lease lease = store.acquire("repo", "owner");

		assertThat(lease.key()).isEqualTo("repo");
		assertThat(lease.owner()).isEqualTo("owner");
		assertThat(lease.token()).isNotBlank();
	}

	@Test
	void acquire_whenSameOwner_shouldReturnSameLease() {
		Lease first = store.acquire("repo", "owner");

		Lease second = store.acquire("repo", "owner");

		assertThat(second.token()).isEqualTo(first.token());
	}

	@Test
	void acquire_whenOtherOwner_shouldThrowBusy() {
		store.acquire("repo", "owner-1");

		LockBusyException error = catchThrowableOfType(() -> store.acquire("repo", "owner-2"), LockBusyException.class);

		assertThat(error.owner()).isEqualTo("owner-1");
	}

	@Test
	void release_whenTokenMatches_shouldFreeLock() {
		Lease lease = store.acquire("repo", "owner-1");

		store.release("repo", lease.token());

		assertThat(store.peek("repo")).isEmpty();
	}

	@Test
	void acquire_whenLeaseExpired_shouldAllowNewOwner() throws Exception {
		store = storeWithLease(Duration.ofMillis(30));
		store.acquire("repo", "owner-1");

		Thread.sleep(100);

		Lease second = store.acquire("repo", "owner-2");

		assertThat(second.owner()).isEqualTo("owner-2");
	}

	@Test
	void acquire_whenConcurrentDifferentOwners_shouldGrantOnlyOneLease() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);

		Future<Object> first = executor.submit(() -> acquireTask("owner-1", ready, start));
		Future<Object> second = executor.submit(() -> acquireTask("owner-2", ready, start));

		ready.await();
		start.countDown();

		Object firstResult = first.get(10, TimeUnit.SECONDS);
		Object secondResult = second.get(10, TimeUnit.SECONDS);
		executor.shutdownNow();

		assertThat(firstResult instanceof Lease ^ secondResult instanceof Lease).isTrue();
	}

	private Object acquireTask(String owner, CountDownLatch ready, CountDownLatch start) throws Exception {
		ready.countDown();
		start.await();
		try {
			return store.acquire("repo", owner);
		} catch (LockBusyException e) {
			return e;
		}
	}

	private FileLockStore storeWithLease(Duration lease) {
		WorkspaceProperties properties = new WorkspaceProperties(tempDir.resolve("ws"), lockDir, lease);
		WorkspaceLayout layout = new PropertiesWorkspaceLayout(properties);
		return new FileLockStore(layout, properties);
	}
}
