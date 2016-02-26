/*eslint-env node*/
const path = require('path');
const fs = require('fs');
const cheerio = require('cheerio');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const target = path.join(__dirname, 'target/selenograph-server-' + getVersionFromPOM());

module.exports = {
    entry: ['./src/main/frontend/js/app.jsx'],
    output: {
        path: target,
        filename: 'js/app.js'
    },
    devtool: 'source-map',
    module: {
        loaders: [
            {test: /\.(js|jsx)$/, loader: 'babel', exclude: /node_modules/},
            {test: /\.png$/, loader: 'url', query: {limit: 10000}},
            {test: /\.css$/, loader: 'style!css'},
            {test: /\.scss$/, loader: ['style', 'css', 'sass'].join('!')}
        ]
    },
    plugins: [
        new HtmlWebpackPlugin({template: './src/main/frontend/index.html'})
    ],
    devServer: {
        contentBase: target
    },
    debug: true,
    progress: true
};

function getVersionFromPOM() {
    const pom = cheerio.load(fs.readFileSync('pom.xml', 'utf8'));
    return pom('project > parent > version').text();
}