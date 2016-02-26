import atmosphere from 'atmosphere.js';
import debug from 'debug';

var log = debug('selenium-face:atmosphere');
var websocketBaseUrl = '/websocket';

export default class Subscription {
    constructor(url, callback) {
        this.url = websocketBaseUrl + url;
        atmosphere.subscribe({
            url: this.url,
            contentType: 'application/json',
            trackMessageLength: true,
            reconnectInterval: 5000,
            transport: 'websocket',
            onMessage: function(response) {
                log(response.responseBody);
                callback(JSON.parse(response.responseBody));
            }
        });
    }

    destroy() {
        atmosphere.unsubscribeUrl(this.url);
    }
}
