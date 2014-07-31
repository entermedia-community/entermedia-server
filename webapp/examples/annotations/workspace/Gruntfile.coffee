module.exports = (grunt) ->
  pkg: grunt.file.readJSON('package.json')

  grunt.loadNpmTasks('grunt-contrib-less');
  grunt.loadNpmTasks('grunt-contrib-coffee')
  grunt.loadNpmTasks('grunt-contrib-watch')

  grunt.initConfig
    watch:
      coffee:
        files: './*.coffee'
        tasks: ['coffee:compile']
      less:
        files: './*.less'
        tasks: ['less:compile']
      styles:
        files: ['less/**/*.less']
        tasks: ['less']
        options:
          nospawn: true
    options: 
      livereload: true
      spawn: false


    coffee:
      compile:
        expand: true
        flatten: true
        cwd: "#{__dirname}/js/"
        src: ['*.coffee']
        dest: 'js/'
        ext: '.js'

    less:
      compile:
        expand: true
        flatten: true
        cwd: "#{__dirname}/less/"
        src: ['*.less']
        dest: 'css/'
        ext: '.css'
  ###    
      development:
        options:
          compress: true
          yuicompress: true
          optimization: 2
        files:
          # target.css file: source.less file
          "./css/*.css": "./less/*.less"
  ###

  grunt.registerTask 'default', ['coffee', 'watch']