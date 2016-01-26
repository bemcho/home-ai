(ns home-ai.videogl
  (:import (org.lwjgl.opengl Display DisplayMode GL11)
           (org.lwjgl.util.glu GLU)
           (org.lwjgl.opengl GL12)
           org.opencv.core.Mat
           (javax.imageio ImageIO)
           (java.io ByteArrayInputStream)
           (org.opencv.core MatOfByte CvType)
           (org.opencv.imgcodecs Imgcodecs)
           (java.nio ByteBuffer)
           (org.lwjgl BufferUtils)
           (org.opencv.videoio VideoCapture Videoio)
           (org.opencv.imgproc Imgproc))
  (:require [home-ai.opencv :refer :all]))

;; ======================================================================

(defn mat-to-gltexture
  [^Mat mat]
  ;// Generate a number for our textureID's unique handle
  (GL11/glLoadIdentity)
  (let [textureID (GL11/glGenTextures)
        inputColourFormat (if (= (.channels mat) 1) GL11/GL_LUMINANCE GL12/GL_BGR)
        matBytes (byte-array (* (.channels mat) (.width mat) (.height mat)))
        byte-buffer (BufferUtils/createByteBuffer (* (.channels mat) (.width mat) (.height mat)))]
    (do
      (.get mat 0 0 matBytes)
      (.put byte-buffer matBytes)
      (.flip byte-buffer)
      (GL11/glBindTexture GL11/GL_TEXTURE_2D textureID)
      (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_MIN_FILTER GL11/GL_LINEAR) ;
      (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_MAG_FILTER GL11/GL_LINEAR) ;
      (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_WRAP_S GL11/GL_CLAMP) ;
      (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_WRAP_T GL11/GL_CLAMP) ;
      (GL11/glTexImage2D GL11/GL_TEXTURE_2D 0 GL11/GL_RGB8 (.cols mat) (.rows mat) 0 inputColourFormat GL11/GL_UNSIGNED_BYTE byte-buffer))
    textureID )
  )
;; spinning triangle in OpenGL 1.1
(defn init-window
  [width height title]
  (def globals (ref {:width     width
                     :height    height
                     :title     title
                     :angle     0.0
                     :last-time (System/currentTimeMillis)}))
  (Display/setDisplayMode (DisplayMode. width height))
  (Display/setTitle title)
  (Display/create))

(defn init-gl
  []
  (GL11/glClearColor 0.0 0.0 0.0 0.0)
  )

(defn draw
  [^Mat imageMat]
  (let [texture-id (mat-to-gltexture imageMat)]

    (do
      (GL11/glLoadIdentity)
      (GL11/glTranslatef 0.0 0.0 -8.0)
      (GL11/glBegin GL11/GL_QUADS)
      (GL11/glBindTexture GL11/GL_TEXTURE_2D texture-id)
      (GL11/glEnd)
      )
    )
  )

(defn update-frame-gl
  [^Mat imageMat]
  (draw imageMat))

(defn run
  []
  (init-window 1920 1080 "Home-ai")
  (init-gl)
  (let [cam (VideoCapture. 1)]

           (doto cam
             ;(.set Videoio/CAP_PROP_FRAME_COUNT 30)
             (.set Videoio/CV_CAP_PROP_FRAME_WIDTH 1920)
             (.set Videoio/CV_CAP_PROP_FRAME_HEIGHT 1080)
             )
           (while (not (Display/isCloseRequested))
             (update-frame-gl (capture-from-cam cam))
             (Display/update)
             (Display/sync 60))
           (do
             (.release cam)
             (Display/destroy)
             )
           )
  )



(defn main
  []
  (println "Run example Alpha")
  (.start (Thread. run)))

