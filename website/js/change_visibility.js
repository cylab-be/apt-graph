function hide(elements) {
  elements = elements.length ? elements : [elements];
  for (var index = 0; index < elements.length; index++) {
    elements[index].style.display = 'none';
  }
}

function show(elements, specifiedDisplay) {
  var computedDisplay;
  var element;
  var index;

  elements = elements.length ? elements : [elements];
  for (index = 0; index < elements.length; index++) {
    element = elements[index];

    // Remove the element's inline display styling
    element.style.display = '';
    computedDisplay = window.getComputedStyle(element, null)
                                    .getPropertyValue('display');

    if (computedDisplay === 'none') {
      element.style.display = specifiedDisplay || 'block';
    }
  }
}

function toggle(elements, specifiedDisplay) {
  var element;
  var index;

  elements = elements.length ? elements : [elements];
  for (index = 0; index < elements.length; index++) {
    element = elements[index];

    if (isElementHidden(element)) {
      element.style.display = '';

      // If the element is still hidden after removing the inline display
      if (isElementHidden(element)) {
        element.style.display = specifiedDisplay || 'block';
      }
    } else {
      element.style.display = 'none';
    }
  }
  function isElementHidden(element) {
    return window.getComputedStyle(element, null)
                                  .getPropertyValue('display') === 'none';
  }
}
