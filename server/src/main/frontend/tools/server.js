/*eslint-env node */
const _ = require('lodash');
const webpack = require('webpack');
const WebpackDevServer = require('webpack-dev-server');
const proxy = require('http-proxy').createProxyServer();
const config = require('../../../../webpack.config');
const port = 3000 || process.env.PORT;

config.entry.unshift(`webpack-dev-server/client?http://localhost:${port}`, 'webpack/hot/dev-server');
config.plugins.push(new webpack.HotModuleReplacementPlugin());
var compiler = webpack(config);
var server = new WebpackDevServer(compiler, _.assign({}, config.devServer, {
    proxy: {
        '/api/*': 'http://localhost:8080'
    },
    stats: { colors: true },
    inline: true,
    hot: true
}));

server.listeningApp.on('upgrade', function(req, socket) {
    if (req.url.match('/websocket')) {
        proxy.ws(req, socket, {'target': 'ws://localhost:8080'});
    }
});

server.listen(port, function(error) {
    if(error) {
        console.error(error); //eslint-disable-line no-console
    } else {
        console.info('==> 🌎  Listening on port %s. Open up http://localhost:%s/ in your browser.', port, port); //eslint-disable-line no-console
    }
});