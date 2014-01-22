(ns blogen.config
  (:require [clojure.java.io :as io]
            [fs.core :as fs])
  (:import [java.io PushbackReader]))

(def ^:private default-config
  {;; org output & our primary input
   :blog-dir "/home/progo/temp/blog/"
   ;; Where the original material lies
   :original-dir "/home/progo/dokumentit/blog/"
   ;; HTML/enlive templates
   :template-dir "/home/progo/dokumentit/blog/templates/"
   ;; Set of full path patterns that are to be excluded
   :excludes #{#"^.*/templates/.*html$"}
   ;; A set of full path patterns of directories-to-be-excluded
   :dir-excludes #{#"^.+/ltxpng$"
                   #"^.+/templates$"}
   ;; Relative location of static assets (css, js)
   :assets-location "templates/"
   ;; Identifier for Disqus
   :disqus-shortname "progim"
   ;; human readable datetime formats to use
   :date-format "dd.MM.YYYY"
   :datetime-format "dd.MM.YYYY HH:mm"
   ;; A couple of self-describing options
   :site-title "Foobarly adventures"
   :subtitle ""
   })

(defn- read-config-from-file
  "Read from a config file. Should return a map, but if the file has
  extra in it, we'll assert. We do eval in full here!"
  [file]
  {:post [(map? %)]}
  (let [file (fs/expand-home file)]
    (if (fs/file? file)
      (with-open [r (io/reader file)]
        (eval (read (PushbackReader. r))))
      (do
        (when (seq file)
          (println "Warning:" file "not found. Use absolute paths please."))
        {}))))

(def config
  "The configuration for application."
  (atom default-config))

(defn add-map!
  "Merge new map into config."
  [m]
  (swap! config #(merge % m)))

(defn update-config!
  "Make an atomic change to the config, merge existing with whatever is
  new."
  [config-file]
  (add-map! (read-config-from-file config-file)))
