package br.ufg.labtime.authface.repository;

import br.ufg.labtime.authface.model.File;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends CrudRepository<File, Long> {

}
