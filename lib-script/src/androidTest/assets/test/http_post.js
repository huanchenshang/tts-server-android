let body = {
   "email": "eve.holt@reqres.in",
   "password": "cityslicka"
}
let headers = {
   'Content-Type': 'application/json'
}
let resp = http.post('https://reqres.in/api/login', JSON.stringify(body), headers)
let ret = resp.json()

console.log(ret['token']) // QpwL5tke4Pnpja7X4