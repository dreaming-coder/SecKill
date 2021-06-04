// noinspection JSCheckFunctionSignatures

const sections = document.querySelectorAll(".count-down");

for (let i = 0; i < sections.length; i++) {
    window.setInterval(function () {
        countDown(sections[i])
    }, 1000);
}

function countDown(section) {
    let content_div = section.children[0]
    let time_div = section.children[1]
    let start = time_div.children[0].innerText
    let end = time_div.children[1].innerText
    let pay_btn = section.parentNode.nextElementSibling.children[1]

    let startTime = +new Date(start)
    let endTime = +new Date(end)
    let nowTime = +new Date()

    let times = (startTime - nowTime) / 1000;  // 相隔多少秒


    if (nowTime > startTime && nowTime < endTime) {
        for (let i = 1; i < content_div.children.length; i++) {
            content_div.children[i].style.display = 'none'
        }
        content_div.children[0].innerText = "活动进行中"
        pay_btn.disabled = false
    } else if (nowTime > endTime) {
        for (let i = 1; i < content_div.children.length; i++) {
            content_div.children[i].style.display = 'none'
        }
        content_div.children[0].innerText = "活动已结束"
        pay_btn.disabled = true
    } else {
        if (parseInt(times / 60 / 60) < 24) {
            let hour = parseInt(times / 60 / 60 % 24);
            hour = hour < 10 ? '0' + hour : hour;

            let minute = parseInt(times / 60 % 60);
            minute = minute < 10 ? '0' + minute : minute;

            let second = parseInt(times % 60);
            second = second < 10 ? '0' + second : second;

            content_div.children[0].innerText = ""

            content_div.children[1].innerText = hour
            content_div.children[1].style.display = 'block'
            content_div.children[2].innerText = minute
            content_div.children[2].style.display = 'block'
            content_div.children[3].innerText = second
            content_div.children[3].style.display = 'block'

            pay_btn.disabled = true
        } else {
            for (let i = 1; i < content_div.children.length; i++) {
                content_div.children[i].style.display = 'none'
            }
            content_div.children[0].innerText = "活动尚未开始"
            pay_btn.disabled = true

        }

    }
}
