(ns blogen.core
  (:use
   [blogen.config])
  (:require
   [taoensso.timbre :as timbre :refer [log info debug]]
   [fs.core :as fs]
   [blogen.analysis :as analysis]
   [blogen.sort :as sort]
   [blogen.post :as post]
   [blogen.templ :as templ]
   [blogen.versions :as versions]
   [net.cgrand.tagsoup :as enlive-ts]
   [net.cgrand.enlive-html :as html])
  (:gen-class))

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
  [dir file]
  (if (.startsWith file dir)
    (-> (str (:original-dir @config)
             (subs file (count dir)))
        (.replaceFirst "\\.html$" ".org"))))

(defn relative-path
  "Predict a relative path for an absolute path."
  [dir path]
  (-> (subs path (count dir))
      (.replaceFirst "^/" "")))

(defn collect-data
  "Given seq of file names read them and parse into a seq of maps."
  [dir files]
  (for [file files]
    (let [original (original-file-location dir file)
          post (read-html-file file)]
      (merge
       (post/all-info post)
       {:history (versions/history original)}
       {:relative-path (relative-path dir file)
        :original-path original
        :path file}))))

(defn read-files
  "First pass. Collect all files and their data in one data structure.
  These are the posts within their own contexts."
  [dir excludes]
  (->> (search-files dir excludes)
       (collect-data dir)
       ((partial filter post/ready-to-publish?))))

(defn transform!
  "Go through all passes, starting afresh and producing the final result."
  []
  (info "Copying into work...")
  (fs/delete-dir (:out-dir @config))
  (fs/copy-dir (:input-dir @config)
               (:out-dir @config))
  (let [_ (info "Reading the files...")
        posts (read-files (:out-dir @config)
                          (:excludes @config))
        _ (info "Doing analysis on them...")
        posts (analysis/analyze-posts posts)]
    (info "Creating single posts...")
    (doseq [post posts]
      (spit (:path post)
            (apply str (templ/single-post post))))
    (info "Writing tags...")
    (let [tags-dir (str (:out-dir @config)
                        (:tags-dir @config))]
      (fs/mkdir tags-dir)
      (doseq [tag (into #{} (mapcat :tags posts))]
        (spit (str tags-dir tag ".rss")
              (apply str (templ/rss-feed-for-tag tag posts)))
        (spit (str tags-dir tag ".html")
              (apply str (templ/tag-page tag posts)))))
    (info "Writing front page...")
    (spit (str (:out-dir @config)
               "index.rss")
          (apply str (templ/rss-feed-for-all-posts posts)))
    (spit (str (:out-dir @config)
               "index.html")
          (apply str (templ/index-page posts)))
    (spit (str (:out-dir @config)
               "404.html")
          (apply str (templ/notfound-page posts)))
    (info "Writing post listings...")
    (spit (str (:out-dir @config)
               "all.html")
          (apply str (templ/all-posts-page posts)))
    (info "All done!")))

(defn -main
  [& args]
  (transform!))

;; Debug toolsies
(let [dbg-strip-big-bits
      (fn [p]
        (-> p
            (update-in [:related-posts] (partial map :title))
            (assoc :all-tags 'STRIPPED)
            (assoc :content 'STRIPPED)
            (assoc :original-content 'STRIPPED)))]
  (defn- analyze-posts'
    "Debug aux again to help with 2nd phase."
    []
    (let [ap (analysis/analyze-posts
              (read-files (:input-dir @config)
                          (:excludes @config)))]
      (map dbg-strip-big-bits ap))))
