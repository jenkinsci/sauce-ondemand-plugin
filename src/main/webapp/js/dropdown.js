function dropdown(target) {
    target.parentNode.getElementsByClassName("dropdown-content")[0].classList.toggle("show");
}

// close the dropdown if the user clicks outside the button or dropdown content
window.onclick = function(event) {
    if (!event.target.matches('.dropdown-button') && !event.target.parentNode.matches('.dropdown')) {
    var dropdowns = document.getElementsByClassName('dropdown-content');
    var i;
    for (i = 0; i < dropdowns.length; i++) {
      var openDropdown = dropdowns[i];
      if (openDropdown.classList.contains('show')) {
        openDropdown.classList.remove('show');
      }
    }
  }
}