1, download and install node.js for windows from : http://www.nodejs.org/download/
2, install all required libraries
   npm install log4js socket.io express jade requirejs async request
3, install jquery if you are on linux box
   npm install jquery
4, this project built on top of:
    4.1: log4js logger for javascript node.js version
    4.2: socket.io an comet event io library which can help us doing realtime browser/server communication
         which can help us implement grabe data from dealspl.us and push to web browser
    4.3: express an node.js based web framework
    4.4: requirejs an module javascript framework which can managing javascript libraries with dependency
    4.5: jade an template engine similar as freemarker or velocity
    4.6: async an light utility library which can simplify the parallel programming a lot
    4.7: request an library which can help on grab data from internet
    4.8: jquery which help us parsing HTML
         note: since jquery for node.js depends on JSDOM, but jsdom requires some native DLL libraries on windows
               so, when we execute the scrapeit by using node.js windows version, we are not run the jquery logic
               in node.js side, instead, we execute it in browser side, with that can help us development easily
               After we deploy it to Linux, we'll automatically using server side jquery
         means: during development, start the scrape through scrapeit.scrape() from browser console will works fine
5, start the application:
   node app.js
6, access it from web browser:
   http://localhost:3000
