(ns blogen.core
  (:use
   [blogen.config])
  (:require
   [fs.core :as fs]
   [blogen.post :as post]
   [blogen.templ :as templ]
   [net.cgrand.tagsoup :as enlive-ts]
   [net.cgrand.enlive-html :as html]))

(defn read-html-file
  [path]
  (html/html-resource (java.io.FileReader. path)))

(defn collect-files
  "Collect all files to be transformed. Return a seq of
  absolute-pathed filenames."
  [blog-dir excludes]
  (let [excluded? (fn [path]
                    (some
                     (fn [exc] (re-matches exc path)) excludes))]
    (->> (fs/find-files blog-dir #".*\.html$")
         (map (memfn getPath))
         (filter (complement excluded?)))))

;; Two-pass reading. First round, we collect so much stuff in data
;; structures. Second round, we use all that data and write the
;; changes down.

(defn collect-data
  "Given seq of file names read them and parse into a seq of maps."
  [files]
  (for [file files]
    (let [post (read-html-file file)]
      (merge
       (post/all-info post)
       {:history '()}
       {:path file}))))

(defn- post-data-rand
  "Debug aux for examining the data we collect from posts."
  []
  (let [randpost (rand-nth (collect-files blog-dir excludes))]
    (-> (collect-data [randpost])
        first
        (assoc :content 'STRIPPED)
        (assoc :original-content 'STRIPPED))))

(defn transform!
  []
  (let [files-to-process (collect-files blog-dir excludes)
        collected-data (collect-data files-to-process)]
    ;; writeup should be largely routine after all the mangling in
    ;; 'collected-data'
    (doseq [post collected-data]
      (spit (:path post)
            (apply str
             (templ/post-template (:title post) 
                                  (:content post)))))

    ;; then the RSS feeds, tag indices, front page, customized index
    ;; files
    ))