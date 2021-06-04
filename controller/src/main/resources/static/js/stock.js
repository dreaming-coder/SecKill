
const stocks = document.querySelectorAll(".stock");

for (let i = 0; i < stocks.length; i++) {
    window.setInterval(function () {
        getStock(stocks[i])
    }, 1000);
}

function getStock(stock) {
    const goodId = stock.getAttribute("goodId")
    $.ajax({
        url: "/stock/" + goodId,
        success: function (num) {
            stock.innerText = num
        }
    });
}