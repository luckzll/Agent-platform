//收藏本站
function AddFavorite(title, url)
{
    try {
        window.external.addFavorite(url, title);
    }
    catch (e) {
        try {
            window.sidebar.addPanel(title, url, "");
        }
        catch (e) {
            alert("抱歉，您所使用的浏览器无法完成此操作。\n加入收藏失败，请使用 [ Ctrl+D ] 进行添加");
        }
    }
}

// 使用立即执行函数防止变量污染全局作用域
(function() {
  // 防止重复执行
  if (window.confettiInitialized) {
    return;
  }
  window.confettiInitialized = true;

  // 樱花飘落效果
  var confettiShower = [];
  var numConfettis = 100; // 增加数量
  // 樱花粉色系颜色
  var colors = [
    "#ffb7c5", // 樱花粉
    "#ffc0cb", // 粉红
    "#ffd1dc", // 浅粉
    "#f8c8dc", // 淡粉
    "#ffe4e1"  // 玫瑰粉
  ];
  
  function Confetti(container) {
    this.container = container;
    this.w = Math.floor(Math.random() * 10 + 8);
    this.h = this.w * 1;
    this.x = Math.floor(Math.random() * 200); // 0-200% 覆盖整个容器宽度
    this.y = Math.floor(Math.random() * 100);
    this.c = colors[Math.floor(Math.random() * colors.length)];
  }
    
  Confetti.prototype.create = function() {
    var newConfetti = '<div class="confetti" style="bottom:' + this.y + '%; left:' + this.x + '%;width:' +
    this.w + 'px; height:' + this.h + 'px;"><div class="rotate"><div class="askew" style="background-color:' + this.c + '"></div></div></div>';
    this.container.innerHTML += newConfetti;
  };
  
  function animateConfettiForContainer(container) {
    if (!container) return;
      
    for (var i = 1; i <= numConfettis; i++) {
      var confetti = new Confetti(container);
      confetti.create();
    }
    var confettis = container.querySelectorAll('.confetti');
    for (var i = 0; i < confettis.length; i++) {
      var opacity = Math.random() + 0.1;
      var animated = confettis[i].animate([
      { transform: 'translate3d(0,0,0)', opacity: opacity },
      { transform: 'translate3d(20vw,100vh,0)', opacity: 1 }],
      {
        duration: Math.random() * 3000 + 3000,
        iterations: Infinity,
        delay: -(Math.random() * 5000) });
  
      confettiShower.push(animated);
    }
  }
    
  // PC端和移动端都添加飘落效果
  var pcContainer = document.getElementById("xuna");
  var mpContainer = document.getElementById("xuna-mp");
    
  animateConfettiForContainer(pcContainer);
  animateConfettiForContainer(mpContainer);
})();