module.exports = function(grunt) {
  //Do grunt-related things here
  grunt.initConfig({
    git_deploy: {
      your_target: {
        options: {
          url: 'https://github.com/RUCD/apt-graph.git'
        },
        src: '.'
      },
    },
  })
  grunt.loadNpmTasks('grunt-git-deploy');
  grunt.registerTask('default', ['git_deploy']);

};
