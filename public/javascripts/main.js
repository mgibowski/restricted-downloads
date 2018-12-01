onCodeFormSubmit = function () {
    var elAfter = document.getElementById("afterDownloadStarted");
    if (elAfter !== null && elAfter !== undefined) {
        elAfter.classList.remove("hidden")
    }
    var elForm = document.getElementById("formContainer");
    if (elForm !== null && elForm !== undefined) {
        elForm.classList.add("hidden")
    }
};

var el = document.getElementById("codeForm");
if (el !== null && el !== undefined) {
    el.addEventListener("submit", onCodeFormSubmit);
}
