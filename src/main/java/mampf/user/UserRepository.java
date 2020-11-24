package mampf.user;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.util.Streamable;

/**
 * A repository interface to manage {@link User} instances.
 */
interface UserRepository extends CrudRepository<User, Long> {

	/**
	 * Re-declared {@link CrudRepository#findAll()} to return a {@link Streamable} instead of {@link Iterable}.
	 */
	@Override
	Streamable<User> findAll();
}
