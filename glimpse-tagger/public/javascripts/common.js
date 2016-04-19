function sendAjax(url, data) {
  jQuery.ajax({
    url: url,
    type: 'GET',
    data: data,
  })
  .done(function(resp) {
    alert(resp);
  })
  .fail(function() {
    alert("Fail");
  });
}