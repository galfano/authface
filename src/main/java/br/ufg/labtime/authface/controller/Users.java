/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.ufg.labtime.authface.controller;

import br.ufg.labtime.authface.model.User;
import br.ufg.labtime.authface.repository.UserRepository;
import br.ufg.labtime.authface.service.FacialRecognitionService;
import br.ufg.labtime.authface.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created galfano on 15/07/19.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class Users {

    private final UserRepository userRepository;
    private final StorageService storageService;
    private final FacialRecognitionService facialRecognitionService;

    @PutMapping
    public ResponseEntity create(@RequestBody User user) {

        userRepository.save(user);

        return new ResponseEntity<>(user.getId(), HttpStatus.OK);
    }

    @PostMapping("/{id}/file")
    public ResponseEntity uploadPhoto(@RequestParam("file") MultipartFile file, @PathVariable Long id) throws IOException {

        return new ResponseEntity<>(storageService.store(file, id), HttpStatus.OK);

    }

    @PostMapping("/auth")
    public ResponseEntity auth(@RequestParam("file") MultipartFile file) throws IOException {

        Path imageTmpPath = storageService.saveInTmp(file);

        User userFound = facialRecognitionService.recognize(imageTmpPath);

        if(userFound != null) {

            return new ResponseEntity<>(userFound, HttpStatus.OK);
        }

        return new ResponseEntity(HttpStatus.FORBIDDEN);
    }

}
