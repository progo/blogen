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

(letfn [(collect' [root-dir excludes init-pattern type-predicate]
          "Collect items by given search parameters. Return a seq of
          absolute-pathed filenames."
          (let [excluded? (fn [path]
                            (some
                             (fn [exc] (re-matches exc path)) excludes))]
            (->> (fs/find-files root-dir init-pattern)
                 (map (memfn getPath))
                 (filter type-predicate)
                 (filter (complement excluded?)))))]
  (defn search-files
    "Collect all HTML files to convert."
    [root-dir excludes]
    (collect' root-dir excludes #".*\.html$" fs/file?))
  (defn search-directories
    "Collect all directories."
    [root-dir excludes]
    (collect' root-dir excludes #".*" fs/directory?)))

(defn original-file-location
  "Given output HTML file, try to determine the location of original
  .org file. Relies on absolute paths that are found in config."
  [file]
  (if (.startsWith file (:blog-dir @config))
    (-> (str (:original-dir @config)
             (subs file (count (:blog-dir @config))))
        (.replaceFirst "\\.html$" ".org"))))

;;; TODO maybe have depth taken into account
(defn relative-path
  "Predict a relative path for an absolute path."
  [path]
  (-> (subs path (count (:blog-dir @config)))
      (.replaceFirst "^/" "")))

(defn file-depth
  "Calculate the directory depth of the given path. Assumes absolute
  paths that begin with config var `blog-dir'."
  [path]
  (-> path
      relative-path
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
        :relative-path (relative-path file)
        :original-path original
        :path file}))))

(defn read-files
  "First pass. Collect all files and their data in one data structure.
  These are the posts within their own contexts."
  []
  (-> (search-files (:blog-dir @config) (:excludes @config))
      collect-data
      ((partial filter post/ready-to-publish?))))

(defn transform!
  "Go through all passes, starting afresh and producing the final result."
  []
  (let [posts (analysis/analyze-posts (read-files))]
    (doseq [post posts]
      (spit (:path post)
            (apply str (templ/single-post post))))
    ;; TODO then the RSS feeds, tag indices, customized index files...
    ;; Tags
    (doseq [tag (into #{} (mapcat :tags posts))]
      (spit (str (:blog-dir @config)
                 (:tags-dir @config)
                 tag ".html")
            (apply str (templ/tag-page tag posts))))
    ;; Front page
    (spit (str (:blog-dir @config)
               "index.html")
          (apply str (templ/index-page posts)))))

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
