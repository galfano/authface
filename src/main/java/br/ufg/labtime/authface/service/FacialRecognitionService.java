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
package br.ufg.labtime.authface.service;

import br.ufg.labtime.authface.model.User;
import br.ufg.labtime.authface.repository.UserRepository;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.FaceRecognizer;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.bytedeco.opencv.global.opencv_core.CV_32SC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.*;


/**
 * Created galfano on 16/07/19.
 */
@Service
public class FacialRecognitionService {

    private final Path fileStorageLocation;
    private final UserRepository userRepository;

    @Autowired
    public FacialRecognitionService(AppPropertiesService appPropertiesService, UserRepository userRepository) {
        this.userRepository = userRepository;

        this.fileStorageLocation = Paths.get(appPropertiesService.getStorageDir())
                .toAbsolutePath().normalize();
    }

    private Mat captureFace(Path imagePath) {

        Mat imageGray = new Mat();

        Mat imageColor = imread(imagePath.toUri().getPath());

        cvtColor(imageColor, imageGray, COLOR_BGRA2GRAY);

        RectVector detectedFace = new RectVector();

        CascadeClassifier detectorFace = new CascadeClassifier("src/main/resources/haarcascade/haarcascade-frontalface-alt.xml");
        detectorFace.detectMultiScale(imageGray,
                detectedFace,
                1.1,
                1,
                0,
                new Size(150, 150),
                new Size(700, 700));

        detectorFace.close();

        Rect dataFace = detectedFace.get(0);

        rectangle(imageColor, dataFace, new Scalar(0, 0, 255, 0));

        Mat capturedFace = new Mat(imageGray, dataFace);
        resize(capturedFace, capturedFace, new Size(160, 160));

        return capturedFace;

    }

    void generateClassifier(Path imagePath, long userId) {

        Mat capturedFace = captureFace(imagePath);

        Path classifierPath = this.fileStorageLocation.resolve("classificador-lbph.yml");

        LBPHFaceRecognizer lbph = LBPHFaceRecognizer.create();

        if(Files.notExists(classifierPath)) {

            lbph.setRadius(1);
            lbph.setNeighbors(8);
            lbph.setGridX(8);
            lbph.setGridY(8);

        } else {

            lbph.read(classifierPath.toUri().getPath());
        }

        Mat label = new Mat(1, 1, CV_32SC1);

        IntBuffer labelsBuffer = label.createBuffer();
        labelsBuffer.put(0, Integer.parseInt(String.valueOf(userId)));

        MatVector photos = new MatVector(1);
        photos.put(0, capturedFace);

        lbph.update(photos, label);
        lbph.write(classifierPath.toUri().getPath());
        lbph.close();
    }

    public User recognize(Path imagePath) {

        Mat capturedFace = captureFace(imagePath);

        FaceRecognizer faceRecognizer = LBPHFaceRecognizer.create();
        faceRecognizer.setThreshold(50);

        Path classifierPath = this.fileStorageLocation.resolve("classificador-lbph.yml");

        faceRecognizer.read(classifierPath.toUri().getPath());

        IntPointer label = new IntPointer(1);
        DoublePointer confidence = new DoublePointer(1);

        faceRecognizer.predict(capturedFace, label, confidence);
        faceRecognizer.close();

        int predict = label.get(0);

        if (predict == -1) {

            return null;
        }

        long userId = Long.parseLong(String.valueOf(predict));

        Optional<User> userOptional = userRepository.findById(userId);

        return userOptional.orElse(null);
    }
}
