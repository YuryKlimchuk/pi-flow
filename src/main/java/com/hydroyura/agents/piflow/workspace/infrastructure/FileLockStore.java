package com.hydroyura.agents.piflow.workspace.infrastructure;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.hydroyura.agents.piflow.workspace.domain.Lease;
import com.hydroyura.agents.piflow.workspace.domain.LockBusyException;
import com.hydroyura.agents.piflow.workspace.domain.LockStore;
import com.hydroyura.agents.piflow.workspace.domain.WorkspaceGitException;
import com.hydroyura.agents.piflow.workspace.domain.WorkspaceLayout;
import org.springframework.stereotype.Component;

/**
 * Файловое хранилище блокировок: по одному lock-файлу на ключ репозитория.
 */
@Component
public class FileLockStore implements LockStore {

	private final Path lockDirectory;
	private final Duration leaseDuration;
	private final Map<String, Object> monitors = new ConcurrentHashMap<>();

	public FileLockStore(WorkspaceLayout layout, WorkspaceProperties properties) {
		this.lockDirectory = layout.lockDirectory();
		this.leaseDuration = properties.leaseDuration() != null ? properties.leaseDuration() : Duration.ofMinutes(5);
	}

	@Override
	public Lease acquire(String key, String owner) {
		Path lockFile = lockDirectory.resolve(key + ".lock");
		Object monitor = monitors.computeIfAbsent(key, k -> new Object());
		synchronized (monitor) {
			try {
				Files.createDirectories(lockDirectory);
				Optional<Lease> existing = read(lockFile);
				Instant now = Instant.now();
				if (existing.isPresent() && existing.get().expiresAt().isAfter(now)) {
					if (existing.get().owner().equals(owner)) {
						return existing.get();
					}
					throw new LockBusyException(existing.get().owner(), existing.get().expiresAt());
				}
				Lease fresh = new Lease(key, owner, UUID.randomUUID().toString(), now.plus(leaseDuration));
				writeAtomic(lockFile, fresh);
				return fresh;
			} catch (IOException e) {
				throw WorkspaceGitException.filesystem("cannot create lock file: " + lockFile, e);
			}
		}
	}

	@Override
	public void release(String key, String token) {
		Path lockFile = lockDirectory.resolve(key + ".lock");
		Object monitor = monitors.computeIfAbsent(key, k -> new Object());
		synchronized (monitor) {
			try {
				Optional<Lease> existing = read(lockFile);
				if (existing.isPresent() && existing.get().token().equals(token)) {
					Files.deleteIfExists(lockFile);
				}
			} catch (IOException e) {
				throw WorkspaceGitException.filesystem("cannot release lock: " + lockFile, e);
			}
		}
	}

	@Override
	public Optional<Lease> peek(String key) {
		return read(lockDirectory.resolve(key + ".lock"));
	}

	private Optional<Lease> read(Path lockFile) {
		if (!Files.exists(lockFile)) {
			return Optional.empty();
		}
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(lockFile, StandardCharsets.UTF_8)) {
			properties.load(reader);
			String key = properties.getProperty("key");
			String owner = properties.getProperty("owner");
			String token = properties.getProperty("token");
			long expiresAt = Long.parseLong(properties.getProperty("expiresAt"));
			return Optional.of(new Lease(key, owner, token, Instant.ofEpochMilli(expiresAt)));
		} catch (IOException | NumberFormatException | NullPointerException e) {
			return Optional.empty();
		}
	}

	private void writeAtomic(Path lockFile, Lease lease) throws IOException {
		Path tmp = lockFile.resolveSibling(lockFile.getFileName() + ".tmp");
		Properties properties = new Properties();
		properties.setProperty("key", lease.key());
		properties.setProperty("owner", lease.owner());
		properties.setProperty("token", lease.token());
		properties.setProperty("expiresAt", String.valueOf(lease.expiresAt().toEpochMilli()));
		try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
			properties.store(writer, null);
		}
		try {
			Files.move(tmp, lockFile, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(tmp, lockFile, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
