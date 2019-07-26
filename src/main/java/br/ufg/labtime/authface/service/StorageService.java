/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.ufg.labtime.authface.service;

import br.ufg.labtime.authface.model.File;
import br.ufg.labtime.authface.repository.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.UUID;

/**
 * Created kelvin on 30/03/19.
 */
@Service
public class StorageService {

    private static Logger logger = LoggerFactory.getLogger(StorageService.class);

    private final Path fileStorageLocation;

    private final FacialRecognitionService facialRecognitionService;

    private final FileRepository fileRepository;

    @Autowired
    public StorageService(AppPropertiesService appPropertiesService, FacialRecognitionService facialRecognitionService, FileRepository fileRepository) {

        this.facialRecognitionService = facialRecognitionService;
        this.fileRepository = fileRepository;

        this.fileStorageLocation = Paths.get(appPropertiesService.getStorageDir())
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public UUID store( byte[] bytes, long userId) throws IOException {

        Path userFolder = this.fileStorageLocation.resolve(String.valueOf(userId));

        if(Files.notExists(userFolder)) {

            Files.createDirectories(userFolder);
        }

        String filename = StringUtils.cleanPath("foto.jpg");

        Path targetLocation = userFolder.resolve(filename);

        Files.write(targetLocation, bytes);

        File fileObj = new File();
        fileObj.setUri(targetLocation.toUri().getPath());
        fileRepository.save(fileObj);

        facialRecognitionService.generateClassifier(targetLocation, userId);

        return fileObj.getId();
    }

    public Path saveInTmp(byte[] bytes)  throws IOException {

        String filename = StringUtils.cleanPath("foto_".concat(String.valueOf(Calendar.getInstance().getTimeInMillis())).concat(".jpg"));

        Path targetLocation = Path.of("/tmp/").resolve(filename);

        Files.write(targetLocation, bytes);

        return targetLocation;
    }
}
