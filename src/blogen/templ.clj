(ns blogen.templ
  (:use
   [blogen.config])
  (:require
   blogen.sort
   [clj-time.format]
   [net.cgrand.enlive-html :as html]))

(defn- streamify 
  [path]
  (java.io.FileReader. path))
(defn- with-template-dir
  [filename]
  (str (:template-dir @config) "/" filename))
(defn- from-template
  "Take just a file name and produce a FileReader object for enlive to
  use."
  [template-name]
  (streamify
   (with-template-dir template-name)))

(defn with-depth
  "Provide a relative path (presumably) with parent dirs."
  [depth s]
  (str (apply str (repeat depth "../"))
       s))

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

(defn link-to-css
  [depth]
  (html/html
   [:link
    {:rel "stylesheet"
     :type "text/css"
     :href (with-depth depth (str (:assets-location @config)
                                  "main.css"))}]))

(defn make-title
  [s]
  (str s " - " (:site-title @config)))

(defn build-tags
  [tags]
  (html/html
   (for [t tags]
     [:li (str t)])))

(html/defsnippet footer-template (from-template "post.html") [:#footer]
  [post])

(html/defsnippet header-template (from-template "post.html") [:#header]
  [post]
  [:#site-title] (html/content (:site-title @config)))

(html/defsnippet head-template (from-template "post.html") [:head]
  [post]
  [:title] (html/content (make-title (:title post)))
  [:link] (html/substitute (link-to-css (:path-depth post))))

(defn post-last-modified
  "Return the last modification date, minor or major."
  [post]
  (-> post :history first :date))

(html/defsnippet sidebar-template (from-template "post.html") [:#sidebar]
  [post]
  [:#post-created] (html/content (format-date (:created post)))
  [:#tags] (html/content (build-tags (:tags post)))
  [:#post-modified] (html/content (format-date (post-last-modified post))))

(html/defsnippet disqus-template (from-template "disqus.html") [:#disqus]
  [uid]
  [:script] (html/transform-content
             (html/replace-vars
              {:sb-shortname (:disqus-shortname @config)
               :sb-identifier (str uid)})))

(html/deftemplate post-template (from-template "post.html")
  [post]
  [:head] (html/substitute (head-template post))
  [:#header] (html/substitute (header-template post))
  [:#sidebar] (html/substitute (sidebar-template post))
  [:#footer] (html/substitute (footer-template post))
  [:#comments] (html/content (disqus-template (:uid post)))
  [:#content] (html/substitute (:content post)))

(defn new-post?
  "Check post's revisions to see if the post is all new or an update."
  [post]
  (->> post
       :history
       (filter :major?)
       count
       zero?))

(html/deftemplate index-template (from-template "index.html")
  [posts]
  [:title] (html/append (make-title ""))
  [:link] (html/substitute (link-to-css 0))
  [:#header] (html/substitute (header-template nil))
  [:#newest-changes :ul :li]
  (html/clone-for
   [p (take 5 (sort blogen.sort/by-latest-major-revision posts))]
   [:.post-link] (html/set-attr :href (:relative-path p))
   [:.post-taste] (html/content (:taste p))
   [:.post-rev-date] (html/content (format-date (post-last-modified p)))
   [:.post-is-updated] #(when (not (new-post? p)) %)
   [:.post-is-new] #(when (new-post? p) %)
   [:.post-name] (html/content (:title p))))

(html/deftemplate tag-page-template (from-template "tag.html")
  [tag posts]
  [:.tag-name] (html/content tag)
  )

(defn single-post
  "Build a complete page from given post."
  [post]
  (post-template post))

(defn index-page
  "Create an index page from posts."
  [posts]
  (index-template posts))

(defn tag-page
  "Render a tag page for this given tag."
  [tag posts]
  (tag-page-template tag posts))
