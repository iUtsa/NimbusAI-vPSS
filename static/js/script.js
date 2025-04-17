// Nimbus AI-PSS Version - JavaScript Client

$(document).ready(function() {
    // DOM Elements
    const chatMessages = $('#chat-messages');
    const userInput = $('#user-input');
    const sendButton = $('#send-button');
    const uploadForm = $('#upload-form');
    const fileUpload = $('#file-upload');
    const uploadButton = $('#upload-button');
    const vizContainer = $('#visualization-container');
    const vizContent = $('#visualization-content');
    const downloadBtn = $('#download-btn');
    const rawDataContainer = $('#raw-data-container');
    const rawDataContent = $('#raw-data-content');
    const toggleRawBtn = $('#toggle-raw-data');  // This one was missing or not defined

    
    // Variables
    let currentFile = null;
    let dataAnalyzed = false;
    let chatHistory = [];
    let currentSessionId = null;

    
    // Add message to chat
    function addMessage(content, isUser = false) {
        const messageType = isUser ? 'user' : 'bot';
        const messageHtml = `
            <div class="message ${messageType}">
                <div class="message-content">
                    <p>${content}</p>
                </div>
            </div>
        `;
        chatMessages.append(messageHtml);
        chatMessages.scrollTop(chatMessages[0].scrollHeight);
        
        // Store in chat history
        chatHistory.push({
            content: content,
            isUser: isUser
        });
    }
    
    // Handle user message submission
    function handleUserMessage() {
        const message = userInput.val().trim();
        if (message === '') return;
        
        // Add user message to chat
        addMessage(message, true);
        userInput.val('');
        
        // Process message and generate response
        processUserMessage(message);
    }
    
    // Process user message and generate bot response
    function processUserMessage(message) {
        // Show typing indicator
        showTypingIndicator();
        
        // If no file has been uploaded yet
        if (!currentFile && !message.toLowerCase().includes('help')) {
            setTimeout(() => {
                removeTypingIndicator();
                addMessage("Please upload a CSV file first so I can analyze it. You can also ask for help if you need guidance.");
            }, 1000);
            return;
        }
        
        // Send message to server for processing
        $.ajax({
            url: '/api/chat',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                message: message,
                hasFile: !!currentFile,
                fileProcessed: dataAnalyzed,
                session_id: currentSessionId // ✅ Include this // ✅ Include this

            }),
            success: function(response) {
                dataAnalyzed = true;
                currentSessionId = response.session_id; // Save session ID
            
                addMessage(response.message);
            
                if (response.visualization) {
                    updateVisualization(response.visualization);
                }
            
                // Move upload form below the visualization and above chat input
                $('.chat-input-container').before($('#file-upload-container'));
            
                if (response.rawData) {
                    updateRawData(response.rawData);
                }
            }
            
            ,
            error: function() {
                removeTypingIndicator();
                addMessage("Sorry, I encountered an error processing your request. Please try again.");
            }
        });
    }
    
    // Show typing indicator
    function showTypingIndicator() {
        const typingHtml = `
            <div class="message bot typing-indicator">
                <div class="message-content">
                    <p><span class="dot"></span><span class="dot"></span><span class="dot"></span></p>
                </div>
            </div>
        `;
        chatMessages.append(typingHtml);
        chatMessages.scrollTop(chatMessages[0].scrollHeight);
    }
    
    // Remove typing indicator
    function removeTypingIndicator() {
        $('.typing-indicator').remove();
    }
    
    // Update visualization
    function updateVisualization(vizData) {
        vizContainer.show();
        
        // If vizData is an image URL
        if (vizData.type === 'image') {
            vizContent.html(`<img src="${vizData.url}" alt="Data Visualization">`);
            downloadBtn.attr('data-src', vizData.url);
        }
        // If vizData is HTML/SVG content
        else if (vizData.type === 'html') {
            vizContent.html(vizData.content);
        }
    }
    
    // Update raw data display
    // Update the existing updateRawData function
function updateRawData(data) {
    // Make sure the container is initially visible
    rawDataContainer.show();
    
    let formattedData = '';
    
    if (typeof data === 'string') {
        formattedData = data;
    } else if (Array.isArray(data)) {
        formattedData = data.map(row => {
            if (Array.isArray(row)) {
                return row.join(', ');
            } else {
                return JSON.stringify(row);
            }
        }).join('\n');
    } else {
        formattedData = JSON.stringify(data, null, 2);
    }
    
    rawDataContent.html(`<pre>${formattedData}</pre>`);
    
    // Update the toggle button text
    toggleRawBtn.text('Hide Raw Data');
}
    
    // Handle file upload
    uploadForm.on('submit', function(e) {
        e.preventDefault();
        
        const fileInput = fileUpload[0];
        if (fileInput.files.length === 0) {
            addMessage("Please select a CSV file to upload.");
            return;
        }
        
        const file = fileInput.files[0];
        if (file.type !== 'text/csv' && !file.name.endsWith('.csv')) {
            addMessage("Please upload a CSV file. Other file formats are not supported.");
            return;
        }
        
        // Create FormData object
        const formData = new FormData();
        formData.append('file', file);
        
        // Show upload message
        addMessage(`Uploading and analyzing "${file.name}"...`);
        
        // Upload file to server
        $.ajax({
            url: '/api/upload',
            type: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            success: function(response) {
                currentFile = file.name;
                dataAnalyzed = true;
                
                // Add success message
                addMessage(response.message);
                
                // Update visualization if available
                if (response.visualization) {
                    updateVisualization(response.visualization);
                }
                
                // Update raw data if available
                if (response.rawData) {
                    updateRawData(response.rawData);
                }
            },
            error: function() {
                addMessage("Sorry, I encountered an error analyzing your file. Please try again or try a different CSV file.");
            }
        });
    });
    
    // Handle file input change (update selected filename)
    fileUpload.on('change', function() {
        const fileInput = fileUpload[0];
        if (fileInput.files.length > 0) {
            const fileName = fileInput.files[0].name;
            $('.custom-file-upload').text(`Selected: ${fileName}`);
        } else {
            $('.custom-file-upload').html('<span class="upload-icon">📁</span> Choose CSV File');
        }
    });
    
    // Toggle raw data view
    // Modify the toggle raw data function
// Use this as your toggle function for raw data
toggleRawBtn.on('click', function() {
    // Get direct reference to the DOM element
    const rawContainer = document.getElementById('raw-data-container');
    
    // Toggle display directly on the DOM element
    if (rawContainer.style.display === 'none' || rawContainer.style.display === '') {
        rawContainer.style.display = 'block';
        this.textContent = 'Hide Raw Data';
    } else {
        rawContainer.style.display = 'none';
        this.textContent = 'Show Raw Data';
    }
});
    
    // Handle download button click
    downloadBtn.on('click', function() {
        const imgSrc = $(this).attr('data-src');
        if (imgSrc) {
            const link = document.createElement('a');
            link.href = imgSrc;
            link.download = 'nimbus_visualization.png';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        } else {
            addMessage("Sorry, there's no visualization available to download.");
        }
    });
    
    // Event Listeners
    sendButton.on('click', handleUserMessage);
    userInput.on('keypress', function(e) {
        if (e.which === 13) {
            handleUserMessage();
        }
    });
    
    // Initial greeting message is already in the HTML
});


// Add this to your script.js file
function checkRawData() {
    if (currentSessionId) {
        $.ajax({
            url: '/api/chat',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                message: "show raw data",
                hasFile: true,
                fileProcessed: true,
                session_id: currentSessionId
            }),
            success: function(response) {
                console.log("Raw data response:", response);
                // Check if we got any raw data
                if (response.rawData) {
                    console.log("Raw data received. First 100 chars:", 
                        response.rawData.substring(0, 100));
                    rawDataContent.html(`<pre>${response.rawData}</pre>`);
                    rawDataContainer.css('display', 'block');
                    console.log("Raw data container should now be visible");
                } else {
                    console.log("No raw data in response");
                }
            },
            error: function(err) {
                console.error("Error fetching raw data:", err);
            }
        });
    } else {
        console.log("No session ID available - can't fetch raw data");
    }
}

