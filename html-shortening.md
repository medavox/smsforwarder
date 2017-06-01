# HTML Shortening Techniques

Things we can do to reversibly shorten the verbose HTML format, before actual compression takes place

* converting closing tags to ^
* replacing http:// with a single byte
* replacing https:// with a single byte
* replacing <span with a single byte
* replacing <div with a single byte
* replacing (<a href=) with a single byte
* removing all html comments
* removing all <meta> tags
