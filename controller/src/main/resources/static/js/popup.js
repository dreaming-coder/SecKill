// 1. 获取元素
const login = document.querySelector('.login');
const mask = document.querySelector('.login-bg');
const closeBtn = document.querySelector('#closeBtn');
const title = document.querySelector('#title');
const phone = document.querySelector('.phone');
const email = document.querySelector('.email');
const flex_div = document.querySelector(".mail");
const btn = title.parentNode.lastElementChild.firstElementChild

// 2. 点击订单弹出输入框  让 mask 和 login 显示出来
function order(goodId, goodName) {
    document.querySelector("#goodId").value = goodId
    document.querySelector("#commodity").value = goodName
    mask.style.display = 'block';
    login.style.display = 'block';
    flex_div.style.display = 'none'
    login.style.height = "280px";
    title.children[0].innerText = "订单查询";
    phone.focus();

    btn.innerText = "订单查询";


    btn.addEventListener("click", function (e) {
        if (phone.value !== "") {
            if (!/^1[3-9]\d{9}$/.test(phone.value)) {
                alert("手机号码有误，请重填！")
            } else {
                $.ajax({
                    url: "/exist/" + goodId + "/" + phone.value,
                    success: function (isExist) {
                        if (isExist) {
                            document.querySelector("#info-form").action = "/order"
                            document.querySelector("#info-form").submit()
                        } else {
                            alert("该订单不存在！")
                            window.location.href = "/list"
                        }
                    }
                })
            }


        } else {
            alert("请输入手机号！")
        }
    })
}

function pay(goodId, goodName) {
    document.querySelector("#goodId").value = goodId
    document.querySelector("#commodity").value = goodName
    mask.style.display = 'block';
    login.style.display = 'block';
    flex_div.style.display = 'block';
    login.style.height = "320px";
    title.firstElementChild.innerText = "买家信息";
    phone.focus();

    btn.innerText = "立即抢购";

    $.ajax({
        url: "/getPathId/" + goodId,
        success: function (id) {
            document.querySelector("#pathId").value = id
        }
    })
    btn.addEventListener("click", function (e) {
        if (phone.value !== "" && email.value !== "") {
            if (!/^1[3-9]\d{9}$/.test(phone.value)) {
                alert("手机号码有误，请重填！")
            } else {
                $.ajax({
                    url: "/exist/" + goodId + "/" + phone.value,
                    success: function (isExist) {
                        if (isExist) {
                            alert("不能重复订购！")
                            window.location.href = "/list"
                        } else {
                            document.querySelector("#info-form").action = "/seckill"
                            document.querySelector("#info-form").submit()
                        }
                    }
                })

            }
        } else {
            alert("请输入买家信息！")
        }
    })
}


// 3. 点击 closeBtn 就隐藏 mask 和 login 
closeBtn.addEventListener('click', function () {
    mask.style.display = 'none';
    login.style.display = 'none';
})

// 4. 开始拖拽
// (1) 当我们鼠标按下， 就获得鼠标在盒子内的坐标
title.addEventListener('mousedown', function (e) {
    const x = e.pageX - login.offsetLeft;
    const y = e.pageY - login.offsetTop;
    // (2) 鼠标移动的时候，把鼠标在页面中的坐标，减去 鼠标在盒子内的坐标就是模态框的left和top值
    document.addEventListener('mousemove', move)

    function move(e) {
        login.style.left = e.pageX - x + 'px';
        login.style.top = e.pageY - y + 'px';
    }

    // (3) 鼠标弹起，就让鼠标移动事件移除
    document.addEventListener('mouseup', function () {
        document.removeEventListener('mousemove', move);
    })
})