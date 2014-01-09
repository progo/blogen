(ns blogen.core
  (:use
   [blogen.config])
  (:require
   [fs.core :as fs]
   [blogen.analysis :as analysis]
   [blogen.sort :as sort]
   [blogen.post :as post]
   [blogen.templ :as templ]
   [blogen.versions :as versions]
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

(defn original-file-location
  "Given output HTML file, try to determine the location of original
  .org file. Relies on absolute paths that are found in config."
  [file]
  (if (.startsWith file blog-dir)
    (-> (str original-dir
             (subs file (count blog-dir)))
        (.replaceFirst "\\.html$" ".org"))))

(defn file-depth
  "Calculate the directory depth of the given path. Assumes absolute
  paths that begin with config var `blog-dir'."
  [path]
  (-> (subs path (count blog-dir))
      (.replaceFirst "^/" "")
      seq
      ((partial filter #{\/}))
      count))

(defn collect-data
  "Given seq of file names read them and parse into a seq of maps."
  [files]
  (for [file files]
    (let [original (original-file-location file)
          post (read-html-file file)]
      (merge
       (post/all-info post)
       {:history (versions/history original)}
       {:path-depth (file-depth file)
        :original-path original
        :path file}))))

(defn read-files
  "First pass. Collect all files and their data in one data structure.
  These are the posts within their own contexts."
  []
  (-> (collect-files blog-dir excludes)
      collect-data
      ((partial filter post/ready-to-publish?))))

(defn transform!
  "Go through all passes, starting afresh and producing the final result."
  []
  (let [posts (read-files)]
    (doseq [post posts]
      (spit (:path post)
            (apply str (templ/single-post post))))
    ;; then the RSS feeds, tag indices, front page, customized index
    ;; files
    ))

;; Debug toolsies
(let [dbg-strip-big-bits
      (fn [p]
        (-> p
            (assoc :content 'STRIPPED)
            (assoc :original-content 'STRIPPED)))]
  (defn- read-files'
    "Debug aux for examining the data we collect from posts."
    []
    (map dbg-strip-big-bits (read-files)))
  (defn- analyze-posts'
    "Debug aux again to help with 2nd phase."
    []
    (let [ap (analysis/analyze-posts (read-files))]
      (map dbg-strip-big-bits ap))))
