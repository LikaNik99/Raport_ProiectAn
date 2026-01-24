// Get the modal
var modal = document.getElementById("myModal");
// Get the button that opens the modal
var btn = document.getElementById("loginBtn");
// Get the <span> element that closes the modal
var span = document.getElementById("close");
// When the user clicks on the button, open the modal
btn.onclick = function () {
    modal.style.display = "block";
}
// When the user clicks on <span> (x), close the modal
span.onclick = function () {
    modal.style.display = "none";
}
// When the user clicks anywhere outside of the modal, close it
window.onclick = function (event) {
    if (event.target == modal) {
        modal.style.display = "none";
    }
}
document.getElementById("UserLogin").onclick = function () {
    if (document.getElementById("uname").value === "admin" && document.getElementById("pname").value === "root") {
        document.getElementById("loginform").action = "AdminPanel.aspx";
        return true;
    } else if (document.getElementById("uname").value === "" || document.getElementById("pname").value === "") {
        alert("Introduceti loginul sau parola");
        return false;
    }
    else {
        alert("Incorect username or password");
        return false;
    }
}