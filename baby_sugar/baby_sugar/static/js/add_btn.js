var btn = $('.btn');
var icn = $('.btn .fa');

btn.on('click', function() {
  if(btn.hasClass("loaded")) {
    btn.removeClass("loaded");
  }
  else {
    btn.addClass("loading");
    icn.addClass("fa-spin");
    setTimeout(function() {
      btn.removeClass("loading");
      btn.addClass("loaded");
      icn.removeClass("fa-spin");
    }, 2000);
  }
});