(function($) {
    jQuery.fn.html5_upload = function(options) {
        function get_file_name(file) {
            return file.webkitRelativePath || file.name || file.fileName;
        }
        function get_file_size(file) {
            return file.size || file.fileSize;
        }
        var available_events = ['filesPicked','setFiles','onStart', 'onStartOne', 'onProgress', 'onFinishOne', 'onFinish', 'onError'];
        var allfiles = null;
        
        var options = jQuery.extend({
            onStart: function(event, total) {
                return true;
            },
            setFiles: function(event, inFiles )
            {
            	setFiles( inFiles );
            },
            onStartOne: function(event, name, number, total) {
                return true;
            },
            onProgress: function(event, progress, name, number, total) {
            },
            onFinishOne: function(event, response, name, number, total) {
            },
            onFinish: function(event, total) {
            },
            onError: function(event, name, error) {
            },
            onBrowserIncompatible: function() {
                alert("Sorry, but your browser is incompatible with uploading files using HTML5 (at least, with current preferences.\n Please install the latest version of Firefox, Safari or Chrome");
            },
            autostart: true,
            autoclear: true,
            stopOnFirstError: false,
            sendBoundary: false,
            fieldName: 'user_file[]',//ignore if sendBoundary is false
            extraFields: {}, // extra fields to send with file upload request (HTML5 only)
            method: 'post',

            STATUSES: {
                'STARTED'   : 'Started',
                'PROGRESS'  : 'Uploading',
                'LOADED'    : 'Loaded',
                'FINISHED'  : 'Finished'
            },
            headers: {
                "Cache-Control":"no-cache",
                "X-Requested-With":"XMLHttpRequest",
                "X-File-Name": function(file){return encodeURI( get_file_name(file))},
                "X-File-Size": function(file){return get_file_size(file)},
                "X-CSRF-Token": $('meta[name="csrf-token"]').attr("content"),
                "Content-Type": function(file){
                    if (!options.sendBoundary) return 'multipart/form-data';
                    return false;
                }
            },


            setName: function(text) {},
            setStatus: function(text) {},
            setProgress: function(value) {},

            genName: function(file, number, total) {
                return file + "(" + (number+1) + " of " + total + ")";
            },
            genStatus: function(progress, finished) {
                if (finished) {
                    return options.STATUSES['FINISHED'];
                }
                if (progress == 0) {
                    return options.STATUSES['STARTED'];
                }
                else if (progress == 1) {
                    return options.STATUSES['LOADED'];
                }
                else {
                    return options.STATUSES['PROGRESS'];
                }
            },
            genProgress: function(loaded, total) {
                return loaded / total;
            }
        }, options);

		function setFiles(inAllFiles)
		{
			allfiles = inAllFiles;
		}

        function upload() {
            var files = allfiles;
            var total = files.length;
            var $this = $(this);
            if (!$this.triggerHandler('html5_upload.onStart', [total,files])) {
                return false;
            }
            this.disabled = true;
            var uploaded = 0;
            var xhr = this.html5_upload['xhr'];
            this.html5_upload['continue_after_abort'] = true;
            function upload_file(number) {
                if (number == total) {
                    $this.triggerHandler('html5_upload.onFinish', [total]);
                    options.setStatus(options.genStatus(1, true));
                    $this.attr("disabled", false);
                    if (options.autoclear) {
                        $this.val("");
                    }
                    return;
                }
                var file = files[number];
                if (!$this.triggerHandler('html5_upload.onStartOne', [get_file_name(file), number, total])) {
                    return upload_file(number+1);
                }
                options.setStatus(options.genStatus(0));
                options.setName(options.genName(get_file_name(file), number, total));
                options.setProgress(options.genProgress(0, get_file_size(file)));
                xhr.upload['onprogress'] = function(rpe) {
                    $this.triggerHandler('html5_upload.onProgress', [rpe.loaded / rpe.total, get_file_name(file), number, total]);
                    options.setStatus(options.genStatus(rpe.loaded / rpe.total));
                    options.setProgress(options.genProgress(rpe.loaded, rpe.total));
                };
                xhr.onload = function(load) {
                    if (xhr.status != 200) {
                        $this.triggerHandler('html5_upload.onError', [get_file_name(file), load]);
                        if (!options.stopOnFirstError) {
                            upload_file(number+1);
                        }
                    }
                    else {
                        $this.triggerHandler('html5_upload.onFinishOne', [xhr.responseText, get_file_name(file), number, total]);
                        options.setStatus(options.genStatus(1, true));
                        options.setProgress(options.genProgress(get_file_size(file), get_file_size(file)));
                        upload_file(number+1);
                    }
                };
                xhr.onabort = function() {
                    if ($this[0].html5_upload['continue_after_abort']) {
                        upload_file(number+1);
                    }
                    else {
                        $this.attr("disabled", false);
                        if (options.autoclear) {
                            $this.val("");
                        }
                    }
                };
                xhr.onerror = function(e) {
                    $this.triggerHandler('html5_upload.onError', [get_file_name(file), e]);
                    if (!options.stopOnFirstError) {
                        upload_file(number+1);
                    }
                };
                xhr.open(options.method, typeof(options.url) == "function" ? options.url(number) : options.url, true);
                $.each(options.headers,function(key,val){
                    val = typeof(val) == "function" ? val(file) : val; // resolve value
                    if (val === false) return true; // if resolved value is boolean false, do not send this header
                    console.log(key + " =  " + val );
                    
                    xhr.setRequestHeader(key, val);
                });

                if (!options.sendBoundary) {
                    xhr.send(file);
                }
                else {
                    if (window.FormData) {//Many thanks to scottt.tw
                        var f = new FormData();
                        f.append(typeof(options.fieldName) == "function" ? options.fieldName() : options.fieldName, file);
                        
                        var myStringArray = typeof(options.extraFields) == "function" ? options.extraFields() : options.extraFields;
                        for (var i = 0; i < myStringArray.length; i++) {
                        	var key = myStringArray[i][0];
                        	var val = myStringArray[i][1];
                        	 f.append(key, val);
                             //alert("appended " + key  + " " + val);
                        }
                        //$.each(options.extraFields, function(key, val){
                        //});
                        xhr.send(f);
                    }
                    else if (file.getAsBinary) {//Thanks to jm.schelcher
                        var boundary = '------multipartformboundary' + (new Date).getTime();
                        var dashdash = '--';
                        var crlf     = '\r\n';

                        /* Build RFC2388 string. */
                        var builder = '';

                        builder += dashdash;
                        builder += boundary;
                        builder += crlf;

                        builder += 'Content-Disposition: form-data; name="'+(typeof(options.fieldName) == "function" ? options.fieldName() : options.fieldName)+'"';

                        //thanks to oyejo...@gmail.com for this fix
                        fileName = unescape(encodeURIComponent(get_file_name(file))); //encode_utf8

                        builder += '; filename="' + fileName + '"';
                        builder += crlf;

                        builder += 'Content-Type: ' + file.type;
                        builder += crlf;
                        builder += crlf;

                        /* Append binary data. */
                        builder += file.getAsBinary();
                        builder += crlf;

                        /* Write boundary. */
                        builder += dashdash;
                        builder += boundary;
                        builder += dashdash;
                        builder += crlf;

                        xhr.setRequestHeader('content-type', 'multipart/form-data; boundary=' + boundary);
                        xhr.sendAsBinary(builder);
                    }
                    else {
                        options.onBrowserIncompatible();
                    }
                }
            }
            upload_file(0);
            return true;
        }

        try {
            return this.each(function() {
                this.html5_upload = {
                    xhr:                    new XMLHttpRequest(),
                    continue_after_abort:    true
                };
                
                $(this).bind('change', function() 
           		{
            		$(this).triggerHandler('html5_upload.filesPicked', [this.files]);
            		if (options.autostart) 
            		{
            			upload();
            		}
            	});

                for (eventname in available_events) {
                        var name = available_events[eventname];
                        var func = options[name];
                    if (func) {
                        $(this).bind("html5_upload."+name, func);
                    }
                }

                $(this)
                    .bind('html5_upload.start', upload)
                    .bind('html5_upload.cancelOne', function() {
                        this.html5_upload['xhr'].abort();
                    })
                    .bind('html5_upload.cancelAll', function() {
                        this.html5_upload['continue_after_abort'] = false;
                        this.html5_upload['xhr'].abort();
                    })
                    .bind('html5_upload.destroy', function() {
                        this.html5_upload['continue_after_abort'] = false;
                        this.xhr.abort();
                        delete this.html5_upload;
                        $(this).unbind('html5_upload.*').unbind('change', upload);
                    });
            });
        }
        catch (ex) {
            options.onBrowserIncompatible();
            return false;
        }
    };
})(jQuery);
