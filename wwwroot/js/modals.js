const myObjectString2 = localStorage.getItem('objectGreeting');
const LoginName = JSON.parse(myObjectString2);

// Get the modal
var VacModal = document.getElementById("VacanciesModal");
var HirModal = document.getElementById("HiringModal");
var UserModal = document.getElementById("UsersModal");
var EmpModal = document.getElementById("EmployeesModal");
var DicModal = document.getElementById("DictionaryModal");
var UnameModal = document.getElementById("UsernameModal");
var VacCustom = document.getElementById("VacanciesCustom");
var VacCustomAdd = document.getElementById("VacanciesCustomModal-add");
var VacCustomDel = document.getElementById("VacanciesCustomModal-delete");

// Get the button that opens the modal
var VacBtn = document.getElementById("VacanciesModal-btn");
var HirBtn = document.getElementById("HiringModal-btn");
var UserBtn = document.getElementById("UsersModal-btn");
var EmpBtn = document.getElementById("EmployeesModal-btn");
var DicBtn = document.getElementById("DictionaryModal-btn");
var UnameBtn = document.getElementById("UsernameModal-btn");
var VacCustomBtn = document.getElementById("VacanciesCustomBtn");
var VacCustomAddBtn = document.getElementById("VacanciesCustomAddBtn");
var VacCustomDelBtn = document.getElementById("VacanciesCustomDeleteBtn");
var VacCustomWiewBtn = document.getElementById("VacanciesCustomWiewBtn");

// Get the <span> element that closes the modal
var span = document.getElementById("close");

if (LoginName === "admin") {
    UserBtn.style.display = "block";
    EmpBtn.style.display = "block";
    DicBtn.style.display = "block";
}

VacCustomBtn.onclick = function () {
    VacCustom.style.display = "block";
}

// When the user clicks on the button, open the modal
VacCustomAddBtn.onclick = function () {
    document.getElementById("VacanciesModal-body").style.display = "none";
    VacCustom.style.display = "none";
    VacModal.style.display = "block";
    HirModal.style.display = "none";
    UserModal.style.display = "none";
    EmpModal.style.display = "none";
    DicModal.style.display = "none";
    UnameModal.style.display = "none";
    VacCustomAdd.style.display = "block";
    VacCustomDel.style.display = "none";
}
VacCustomDelBtn.onclick = function () {
    document.getElementById("VacanciesModal-body").style.display = "none";
    VacCustom.style.display = "none";
    VacModal.style.display = "block";
    HirModal.style.display = "none";
    UserModal.style.display = "none";
    EmpModal.style.display = "none";
    DicModal.style.display = "none";
    UnameModal.style.display = "none";
    VacCustomAdd.style.display = "none";
    VacCustomDel.style.display = "block";
}
VacBtn.onclick = function () {
    document.getElementById("VacanciesModal-body").style.display = "block";
    VacModal.style.display = "block";
    HirModal.style.display = "none";
    UserModal.style.display = "none";
    EmpModal.style.display = "none";
    DicModal.style.display = "none";
    UnameModal.style.display = "none";
    VacCustomAdd.style.display = "none";
    VacCustomDel.style.display = "none";
}
VacCustomWiewBtn.onclick = function () {
    document.getElementById("VacanciesModal-body").style.display = "block";
    VacModal.style.display = "block";
    HirModal.style.display = "none";
    UserModal.style.display = "none";
    EmpModal.style.display = "none";
    DicModal.style.display = "none";
    UnameModal.style.display = "none";
    VacCustomAdd.style.display = "none";
    VacCustomDel.style.display = "none";
}
HirBtn.onclick = function () {
    VacModal.style.display = "none";
    HirModal.style.display = "block";
    UserModal.style.display = "none";
    EmpModal.style.display = "none";
    DicModal.style.display = "none";
    UnameModal.style.display = "none";
    VacCustomAdd.style.display = "none";
    VacCustomDel.style.display = "none";
}
UserBtn.onclick = function () {
    VacModal.style.display = "none";
    HirModal.style.display = "none";
    UserModal.style.display = "block";
    EmpModal.style.display = "none";
    DicModal.style.display = "none";
    UnameModal.style.display = "none";
    VacCustomAdd.style.display = "none";
    VacCustomDel.style.display = "none";
}
EmpBtn.onclick = function () {
    VacModal.style.display = "none";
    HirModal.style.display = "none";
    UserModal.style.display = "none";
    EmpModal.style.display = "block";
    DicModal.style.display = "none";
    UnameModal.style.display = "none";
    VacCustomAdd.style.display = "none";
    VacCustomDel.style.display = "none";
}
DicBtn.onclick = function () {
    VacModal.style.display = "none";
    HirModal.style.display = "none";
    UserModal.style.display = "none";
    EmpModal.style.display = "none";
    DicModal.style.display = "block";
    UnameModal.style.display = "none";
    VacCustomAdd.style.display = "none";
    VacCustomDel.style.display = "none";
}
UnameBtn.onclick = function () {
    UnameModal.style.display = "block";
    document.getElementById("TextUser").innerHTML = "Welcome " + LoginName;
}

// When the user clicks on <span> (x), close the modal
span.onclick = function () {
    UnameModal.style.display = "none";
}

// When the user clicks anywhere outside of the modal, close it
window.onclick = function (event) {
    if (event.target == UnameModal) {
        UnameModal.style.display = "none";
    }
    if (event.target == VacCustom) {
        VacCustom.style.display = "none";
    }
}