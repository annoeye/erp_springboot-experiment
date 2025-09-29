import http from "k6/http";
import {check, sleep} from 'k6';

export let options = {
    vus: 5000,
    duration: '10s',
};

export default function () {
    let res = http.get('http://localhost:8080/api/auth/get-user/{type}?page=1&size=10&type=GET_ALL')

    check(res, {
        'status: 200' : (r) => r.status === 200,
    });

    sleep(1)
}