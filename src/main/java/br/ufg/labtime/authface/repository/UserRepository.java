package br.ufg.labtime.authface.repository;

import br.ufg.labtime.authface.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

}
