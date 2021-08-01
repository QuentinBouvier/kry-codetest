const listContainer = document.querySelector('#service-list');
let servicesRequest = new Request('/service');
fetch(servicesRequest)
    .then(function (response) {
        return response.json();
    })
    .then(function (serviceList) {
        serviceList.forEach(service => {
            var li = document.createElement("li");
            li.innerHTML = `<strong>${service.status.padEnd(7, ' ')}</strong> - `;
            li.appendChild(document.createTextNode(`${service.name} (${service.url})`));
            listContainer.appendChild(li);
        });
    });

const saveButton = document.querySelector('#post-service');
saveButton.onclick = () => {
    document.querySelector('#error-container').innerHTML = '';
    let urlName = document.querySelector('#url-name').value;
    let serviceName = document.querySelector('#service-name').value;
    fetch('/service', {
        method: 'post',
        headers: {
            'Accept': 'application/json, text/plain, */*',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({url: urlName, name: serviceName})
    }).then(res => {
        if (res.ok) {
            location.reload()
        } else {
            res.text().then(body => {
                document.querySelector('#error-container').innerHTML = body;
            })
        }
    });
}