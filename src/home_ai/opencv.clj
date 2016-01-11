(ns home-ai.opencv
    (:import
      org.opencv.core.Core
      org.opencv.core.MatOfRect
      org.opencv.core.MatOfByte
      org.opencv.core.Point
      org.opencv.core.Rect
      org.opencv.core.Mat
      org.opencv.core.Scalar
      org.opencv.imgcodecs.Imgcodecs
      org.opencv.objdetect.CascadeClassifier
      org.opencv.objdetect.Objdetect
      org.opencv.videoio.VideoCapture
      java.awt.image.BufferedImage
      java.awt.RenderingHints
      javax.imageio.ImageIO
      java.io.File
      java.io.ByteArrayInputStream
      org.opencv.core.Size (org.opencv.imgproc Imgproc)) 
    (:require [clojure.walk :as walk]))


(defn convert-mat-to-buffer-image [mat]
  (let [new-mat (MatOfByte.)]
    (Imgcodecs/imencode ".png" mat new-mat)
    (ImageIO/read (ByteArrayInputStream. (.toArray new-mat)))))

(def all-detections-for-image (atom []))
(def face-detections (atom []))

(declare classifiers)


(defn create-classifier-agent [classifier]
  (agent {:detections (MatOfRect.)
          :classifier classifier}))

(defn load-classifier [file-path]
  (walk/keywordize-keys (assoc {} (last (clojure.string/split file-path #"/")) (create-classifier-agent (CascadeClassifier.
                                                                                                          file-path)))))

(defn load-image [filename]
  (Imgcodecs/imread filename))

(defn detect-faces! [classifier image]
  (.detectMultiScale classifier
                     image
                     @face-detections))

(defn detect-faces-agent! [a image]
  (.detectMultiScale (:classifier a)
                     image
                     (:detections a)
                     )
  ;(reset! all-detections-for-image (concat @all-detections-for-image (.toArray @detections)))
  a
    )

(defn draw-bounding-boxes!
  [image]
  (doall (map (fn [rect]
                (Imgproc/rectangle image
                                   (Point. (.x rect) (.y rect))
                                   (Point. (+ (.x rect) (.width rect))
                                           (+ (.y rect) (.height rect)))
                                   (Scalar. 0 255 0)))
              @all-detections-for-image)
         )
  ;(Imgcodecs/imwrite "faceDetections.png" image)
  (convert-mat-to-buffer-image image))

(defn load-classifiers
  [dir-name]
  (loop [file-paths (filter #(.endsWith (.getName %) ".xml") (.listFiles (java.io.File. dir-name)) )
         result {}]
    (if-not (first file-paths)
      result
      (recur
        (rest file-paths)
        (merge result (load-classifier (str dir-name (.getName (first file-paths)))))))))

(defn process-mat-and-return-image [imageMat]
  (reset! all-detections-for-image [])
  (let [imageMatGray (Mat.)
        agents (vals @classifiers)]
    (Imgproc/cvtColor imageMat imageMatGray Imgproc/COLOR_BGR2GRAY)
    (Imgproc/equalizeHist imageMatGray imageMatGray)
    (dorun
     (doseq [agent agents]
       (send agent detect-faces-agent! imageMatGray )
       (doall (map #(reset! all-detections-for-image (concat @all-detections-for-image (.toArray (:detections (deref %)))))   agents))))
    (draw-bounding-boxes! imageMat)))

  (defn capture-from-cam
    "Gets frame from cam and returns it as Mat."
    [cam]
    (let [matImage (Mat.)]
      (when (.isOpened cam)
        (do
          (.read cam matImage))) matImage))

(defn capture-from-device [n]
  (capture-from-cam (VideoCapture. n)))
;(process-and-return-image "opencvin.png")

(defn init-opencv []
  (def classifiers (atom (load-classifiers "resources/data/classifiers/"))))


