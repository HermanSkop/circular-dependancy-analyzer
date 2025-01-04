document.getElementById("directory").addEventListener("change", function () {
    const label = document.querySelector("label[for='directory']");
    const files = Array.from(this.files);
    const validExtensions = [".py", ".js"];
    const errorMessageElement = document.querySelector(".error-message");

    if (errorMessageElement)
        errorMessageElement.textContent = "";

    const filteredFiles = files.filter(file => {
        const fileExtension = file.name.slice(file.name.lastIndexOf(".")).toLowerCase();
        return validExtensions.includes(fileExtension);
    });

    if (filteredFiles.length === 0) {
        errorMessageElement.textContent = "No valid files found in the selected directory.";
        label.textContent = "Select a directory";
        return;
    }
    label.textContent = `${filteredFiles.length} valid files selected`;
    this.files = filteredFiles;
});
