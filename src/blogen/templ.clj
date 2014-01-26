(ns blogen.templ
  (:use
   [blogen.config])
  (:require
   blogen.sort
   [clj-time.format]
   [net.cgrand.enlive-html :as html]))

(defn- from-template
  "Take just a file name and produce a FileReader object for enlive to
  use."
  [template-name]
  (java.io.FileReader.
   (str (:template-dir @config) "/" template-name)))

(defn from-base-url
  "Build an absolute url."
  [s]
  (str (:base-url @config) s))

(defn- format-date-with-fmt
  "Format a DateTime object to human readable string, using
  `format-string'."
  [format-string dt]
  (clj-time.format/unparse
   (clj-time.format/formatter format-string)
   dt))

(def format-date
  "DateTime (just date) -> human readable"
  (partial format-date-with-fmt
           (:date-format @config)))

(def format-datetime
  "DateTime (date and time) -> human readable"
  (partial format-date-with-fmt
           (:datetime-format @config)))

(def link-to-css
  (html/html
   [:link
    {:rel "stylesheet"
     :type "text/css"
     :href (from-base-url (str (:assets-location @config)
                               "main.css"))}]))

(defn make-title
  [s]
  (str s " — " (:site-title @config)))


;;; Helpful tag utils

(defn tag-class
  "Determine suitable tag class based on its usage"
  [usage]
  (str "tag-"
       (cond
        (<= 0 usage 2) 0
        (<= 3 usage 5) 1
        (<= 6 usage 9) 2
        :else          3)))

(defn build-tags
  "Build an HTML snippet out of tags."
  [tags all-tags]
  (html/html
   (for [t tags]
     [:li {:class (tag-class (all-tags t 0))}
      [:a {:href
           (from-base-url (str (:tags-dir @config)
                               t ".html"))}
       (str t)]])))

(defn has-tag?
  "Create a predicate checking for certain tag."
  [tag]
  (fn [post]
    (->> post
         :tags
         (some #{tag}))))

(defn post-last-modified
  "Return the last modification date, minor or major."
  [post]
  (-> post :history first :date))

(html/defsnippet disqus-template (from-template "disqus.html") [:#disqus]
  [uid]
  [:script] (html/transform-content
             (html/replace-vars
              {:sb-shortname (:disqus-shortname @config)
               :sb-identifier (str uid)})))

;;; All pages that wish to look uniform must apply thru this.
(html/deftemplate from-master (from-template "master.html")
  [{:keys [head header footer sidebar main]}]
  [:head] (html/append (map :content head))
  [:#sidebar] (html/substitute sidebar)
  [:#main] (html/substitute main)
  [:#site-title] (html/content (:site-title @config))
  ;; [:#header] header
  ;; [:#footer] footer
  )

;; Single post.
(html/defsnippets (from-template "post.html")
  [post-content-template [:#main]
   [post]
   [:#comments] (html/content (disqus-template (:uid post)))
   [:#content] (html/substitute (:content post))]
  [post-sidebar-template [:#sidebar]
   [post]
   [:#post-created] (html/content
                     (format-date (:created post)))
   [:#tags] (html/content
             (build-tags (:tags post) (:all-tags post)))
   [:#post-modified] (html/content
                      (format-date (post-last-modified post)))]
  [post-head-template [:head]
   [post]
   [:title] (html/content (make-title (:title post)))
   [:link] (html/substitute link-to-css)])

(defn new-post?
  "Check post's revisions to see if the post is all new or an update."
  [post]
  (->> post
       :history
       (filter :major?)
       count
       zero?))

;;;; Post listings

;; Feel free to stuff this with different crap
(defn transform-post-list-item
  [p]
  (html/transformation
   [:.post-link]
   (html/set-attr :href (from-base-url (:relative-path p)))
   [:.post-taste]
   (html/content (:taste p))
   [:.tag-list-oneline]
   (html/content (build-tags (:tags p) (:all-tags p)))
   [:.post-rev-date]
   (html/content (format-date (post-last-modified p)))
   [:.post-is-updated]
   #(when (not (new-post? p)) %)
   [:.post-is-new]
   #(when (new-post? p) %)
   [:.post-name]
   (html/content (:title p))))

;; Index page
(html/defsnippets (from-template "index.html")
  [index-head-template [:head]
   [posts]
   [:title] (html/append (make-title ""))
   [:link] (html/substitute link-to-css)]
  [index-content-template [:#main]
   [posts]
   [:#newest-changes :ul :li]
   (html/clone-for
    [p (take 5 (sort blogen.sort/by-latest-major-revision posts))]
    (transform-post-list-item p))])

;; Tag pages
(html/defsnippets (from-template "tag.html")
  [tag-head-template [:head]
   [tag posts]
   [:title] (html/append (make-title ""))
   [:link] (html/substitute link-to-css)]
  [tag-content-template [:#main]
   [tag posts]
   [:.tag-name] (html/content tag)
   [:#article-list :ul :li]
   (html/clone-for
    [p (filter (has-tag? tag) posts)]
    (transform-post-list-item p))])

(defn single-post
  "Build a complete page from given post."
  [post]
  (from-master
   {:head (post-head-template post)
    :sidebar (post-sidebar-template post)
    :main (post-content-template post)
    }))

(defn index-page
  "Create an index page from posts."
  [posts]
  (from-master
   {:main (index-content-template posts)
    :head (index-head-template posts)}))

(defn tag-page
  "Render a tag page for this given tag."
  [tag posts]
  (from-master
   {:head (tag-head-template tag posts)
    :main (tag-content-template tag posts)}))
