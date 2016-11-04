module.exports = function(grunt) {
  //Do grunt-related things here
  grunt.initConfig({
    git_deploy: {
      your_target: {
        options: {
          url: 'git@github.com:example/repo.git'
        },
        src: 'directory/to/deploy'
      },
    },
  })
  grunt.loadNpmTasks('grunt-git-deploy');

};
