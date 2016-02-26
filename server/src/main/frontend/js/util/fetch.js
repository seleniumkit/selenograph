import fetch from 'isomorphic-fetch';

function checkStatus(response) {
    if (response.ok) {
        return response;
    } else {
        throw new Error(response.statusText);
    }
}

function parseJSON(response) {
    return response.json();
}

export default function enhancedFetch(url, options = {}) {
    options.headers = Object.assign({
        'Accept': 'application/json',
        'Content-Type': 'application/json'
    }, options.headers);
    if (typeof options.body !== 'string') {
        options.body = JSON.stringify(options.body);
    }
    return fetch(url, options)
        .then(checkStatus)
        .then(parseJSON);
}