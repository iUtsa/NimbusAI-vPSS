from flask import Flask, render_template, request, jsonify, send_file
import os
import pandas as pd
import tempfile
import uuid
import json
import subprocess
import logging
from werkzeug.utils import secure_filename
import time

start = time.time()


# Configure logging
logging.basicConfig(level=logging.INFO, 
                   format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'uploads')
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16 MB max file size
app.config['JAVA_BIN'] = 'java'  # Update this to your Java path if needed
app.config['TEMP_DIR'] = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'temp')
app.config['VISUALIZATIONS_DIR'] = os.path.join(os.path.dirname(os.path.abspath(__file__)), 
                                               'static', 'visualizations')

# Create directories if they don't exist
os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
os.makedirs(app.config['TEMP_DIR'], exist_ok=True)
os.makedirs(app.config['VISUALIZATIONS_DIR'], exist_ok=True)

# Global data store for user sessions
sessions = {}

# Helper functions
def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() == 'csv'

def process_csv_with_java(file_path, session_id):
    """Process the CSV file with the Java components for plot, salt, smooth, and graph"""
    output_dir = os.path.join(app.config['VISUALIZATIONS_DIR'], session_id)
    os.makedirs(output_dir, exist_ok=True)
    
    output_data = os.path.join(app.config['TEMP_DIR'], f"{session_id}_processed_data.csv")
    
    # Run the Java application
    try:
        cmd = [
                app.config['JAVA_BIN'],
                    '-cp',
                os.pathsep.join([
                    os.path.join('build', 'lib', 'NimbusAI-PSS.jar'),
                    os.path.join('build', 'lib', 'jfreechart-1.5.3.jar'),
                    os.path.join('build', 'lib', 'jcommon-1.0.23.jar'),
                ]),
            'Main',
            file_path,
            output_dir,
            output_data
        ]


        
        logger.info(f"Running Java command: {' '.join(cmd)}")
        
        # Execute the Java application
        result = subprocess.run(cmd, capture_output=True, text=True)
        
        if result.returncode != 0:
            logger.error(f"Java process failed: {result.stderr}")
            return None, None, result.stderr
        
        logger.info(f"Java process output: {result.stdout}")
        
        # Read the processed data
        processed_data = None
        if os.path.exists(output_data):
            processed_data = pd.read_csv(output_data)
        
        # Get the paths to all generated visualizations
        visualizations = {
            'initial': os.path.join(output_dir, 'initial_plot.png'),
            'salted': os.path.join(output_dir, 'salted_plot.png'),
            'smoothed': os.path.join(output_dir, 'smoothed_plot.png'),
            'final': os.path.join(output_dir, 'final_plot.png')
        }
        
        return visualizations, processed_data, None
    
    except Exception as e:
        logger.error(f"Error processing CSV with Java: {str(e)}")
        return None, None, str(e)

def analyze_csv(file_path):
    """Perform basic analysis on the CSV file"""
    try:
        # Read CSV file
        df = pd.read_csv(file_path)
        
        # Basic statistics
        stats = {
            'rows': len(df),
            'columns': len(df.columns),
            'column_names': df.columns.tolist(),
            'numeric_columns': df.select_dtypes(include=['number']).columns.tolist(),
            'categorical_columns': df.select_dtypes(include=['object']).columns.tolist(),
            'missing_values': df.isnull().sum().to_dict(),
            'summary': df.describe().to_dict()
        }
        
        return df, stats, None
    except Exception as e:
        logger.error(f"Error analyzing CSV: {str(e)}")
        return None, None, str(e)

def get_bot_response(message, session_data):
    """Generate a response from the chatbot based on user message and session data"""
    message_lower = message.lower()

    if not session_data.get('file_processed'):
        return {
            'message': "Please upload a CSV file first so I can analyze it."
        }

    # Help message
    if 'help' in message_lower or 'what can you do' in message_lower:
        return {
            'message': "I can help you analyze CSV data using our Plot-Salt-Smooth-Graph pipeline. Here's what you can ask me:\n\n"
                       "- 'Show me the initial plot' - Display the original data visualization\n"
                       "- 'Show me the salted plot' - Display the data after salting\n"
                       "- 'Show me the smoothed plot' - Display the data after smoothing\n"
                       "- 'Show me the final graph' - Display all data series together\n"
                       "- 'Show raw data' - Display the original data\n"
                       "- 'Show processed data' - Display all processed data stages\n"
                       "- 'Explain the pipeline' - Learn about our processing steps\n"
                       "- 'Download visualization' - Get the plot images\n"
                       "- 'Tell me about my data' - Get basic statistics"
        }

    # Explain salting
    if 'explain' in message_lower and 'salt' in message_lower:
        return {
            'message': "The salting procedure adds controlled variability to your data to highlight certain patterns "
                       "that might be obscured in the original data. It works by:\n\n"
                       "1. Analyzing your data to detect trends or cycles\n"
                       "2. Applying different salting methods based on data characteristics\n"
                       "3. For trend data, enhancing the trend with systematic pattern\n"
                       "4. For cyclic data, enhancing the cycles with periodic components\n"
                       "5. For random data, adding controlled random variations\n\n"
                       "This process often makes certain features more visible when the smoothing algorithm is applied."
        }

    # Explain smoothing
    if 'explain' in message_lower and 'smooth' in message_lower:
        return {
            'message': "The Solter smoothing algorithm reduces noise in your data while preserving important patterns. It works by:\n\n"
                       "1. Using a sliding window approach to examine points around each data point\n"
                       "2. Calculating a weighted average based on distance from the center point\n"
                       "3. Applying exponential decay to weights (points further away have less influence)\n"
                       "4. Using adaptive parameters based on local data characteristics\n\n"
                       "This results in smoother data with reduced random fluctuations but preserved significant features."
        }

    # Show initial plot
    if 'initial' in message_lower and ('plot' in message_lower or 'show' in message_lower):
        return {
            'message': "Here's the initial plot of your raw data:",
            'visualization': {
                'type': 'image',
                'url': f"/visualizations/{session_data['session_id']}/initial_plot.png"
            }
        }

    # Show salted plot
    if 'salted' in message_lower and ('plot' in message_lower or 'show' in message_lower):
        return {
            'message': "Here's the plot after applying the salting procedure:",
            'visualization': {
                'type': 'image',
                'url': f"/visualizations/{session_data['session_id']}/salted_plot.png"
            }
        }

    # Show smoothed plot
    if 'smoothed' in message_lower and ('plot' in message_lower or 'show' in message_lower):
        return {
            'message': "Here's the plot after applying the smoothing algorithm:",
            'visualization': {
                'type': 'image',
                'url': f"/visualizations/{session_data['session_id']}/smoothed_plot.png"
            }
        }

    # Show final graph
    if 'final' in message_lower or ('all' in message_lower and 'plot' in message_lower) or 'graph' in message_lower:
        return {
            'message': "Here's the final graph showing original, salted, and smoothed data:",
            'visualization': {
                'type': 'image',
                'url': f"/visualizations/{session_data['session_id']}/final_plot.png"
            }
        }

    # Show raw data
    if 'raw data' in message_lower or 'original data' in message_lower:
        df = session_data.get('original_data')
        if df is not None:
            return {
                'message': "Here's your original data:",
                'rawData': df.head(50).to_string()
            }
        else:
            return {'message': "Sorry, I couldn't retrieve the original data."}

    # Show processed data
    if 'processed data' in message_lower:
        df = session_data.get('processed_data')
        if df is not None:
            return {
                'message': "Here's your data after all processing steps:",
                'rawData': df.head(50).to_string()
            }
        else:
            return {'message': "Sorry, I couldn't retrieve the processed data."}
        

       # Explain the dataset with statistical summary
    if 'explain' in message_lower and ('data' in message_lower or 'dataset' in message_lower):
        stats = session_data.get('stats')
        if stats:
            message = f"Here's an overview of your dataset:\n\n"
            message += f"- Total rows: {stats['rows']}\n"
            message += f"- Total columns: {stats['columns']}\n"
            message += f"- Column names: {', '.join(stats['column_names'][:5])}"
            if len(stats['column_names']) > 5:
                message += "..."
            message += "\n"

            if stats['numeric_columns']:
                message += f"- Numeric columns: {', '.join(stats['numeric_columns'][:5])}"
                if len(stats['numeric_columns']) > 5:
                    message += "..."
                message += "\n"

            if stats['categorical_columns']:
                message += f"- Categorical columns: {', '.join(stats['categorical_columns'][:5])}"
                if len(stats['categorical_columns']) > 5:
                    message += "..."
                message += "\n"

            if any(stats['missing_values'].values()):
                missing = [f"{k}: {v}" for k, v in stats['missing_values'].items() if v > 0]
                message += f"- Missing values in: {', '.join(missing[:3])}"
                if len(missing) > 3:
                    message += "..."
                message += "\n"
            else:
                message += "- No missing values detected.\n"

        # Convert summary to DataFrame and return as text
            try:
                summary_df = pd.DataFrame(stats['summary'])
                describe_text = summary_df.to_string()
                return {
                    'message': message + "\nHere’s the statistical summary of numeric columns:",
                    'rawData': describe_text
            }
            except Exception as e:
                return {'message': message + "\nSummary data could not be formatted."}
        else:
            return {'message': "Sorry, I couldn't retrieve stats from your data."}
    

    # Tell me about my data
    if 'tell' in message_lower and 'data' in message_lower:
        stats = session_data.get('stats')
        if stats:
            message = f"Your data has {stats['rows']} rows and {stats['columns']} columns. "

            if stats['numeric_columns']:
                message += f"Numeric columns: {', '.join(stats['numeric_columns'][:5])}{'...' if len(stats['numeric_columns']) > 5 else ''}. "

            if stats['categorical_columns']:
                message += f"Categorical columns: {', '.join(stats['categorical_columns'][:5])}{'...' if len(stats['categorical_columns']) > 5 else ''}. "

            if any(stats['missing_values'].values()):
                missing = [f"{k}: {v}" for k, v in stats['missing_values'].items() if v > 0]
                message += f"Missing values detected in columns: {', '.join(missing[:3])}{'...' if len(missing) > 3 else ''}."
            else:
                message += "No missing values detected."

            return {'message': message}
        else:
            return {'message': "Sorry, I don't have statistics about your data."}

    # Default fallback
    return {
        'message': "I'm not sure how to help with that. You can ask me to show plots, explain the pipeline, or type 'Help' to see all options."
    }

# Routes
@app.route('/')
def index():
    """Render the main page"""
    return render_template('index.html')

@app.route('/api/upload', methods=['POST'])
def upload_file():
    """Handle file upload and processing"""
    if 'file' not in request.files:
        return jsonify({'error': 'No file part'}), 400
    
    file = request.files['file']
    
    if file.filename == '':
        return jsonify({'error': 'No selected file'}), 400
    
    if file and allowed_file(file.filename):
        session_id = str(uuid.uuid4())
        filename = secure_filename(file.filename)
        file_path = os.path.join(app.config['UPLOAD_FOLDER'], f"{session_id}_{filename}")
        
        # Save the file
        file.save(file_path)
        
        # Analyze the CSV
        df, stats, error = analyze_csv(file_path)
        
        if error:
            return jsonify({
                'error': f'Error analyzing CSV: {error}'
            }), 400
        
        # Process with Java (Plot, Salt, Smooth, Graph)
        visualizations, processed_df, java_error = process_csv_with_java(file_path, session_id)
        
        if java_error:
            return jsonify({
                'error': f'Error processing with Java: {java_error}'
            }), 400
        
        # Store session data
        sessions[session_id] = {
            'session_id': session_id,
            'file_path': file_path,
            'original_data': df,
            'processed_data': processed_df,
            'visualizations': visualizations,
            'stats': stats,
            'file_processed': True
        }
        
        # Return success with visualization
        return jsonify({
            'message': f"I've analyzed your CSV file and applied our Plot-Salt-Smooth-Graph pipeline. "
                       f"Your file has {len(df)} rows and {len(df.columns)} columns. "
                       f"All visualizations are ready!",
            'visualization': {
                'type': 'image',
                'url': f"/visualizations/{session_id}/final_plot.png"
            },
            'session_id': session_id
        })
    
    return jsonify({'error': 'File type not allowed. Please upload a CSV file.'}), 400

@app.route('/api/chat', methods=['POST'])
def chat():
    """Process chat messages"""
    data = request.json
    message = data.get('message', '')
    session_id = data.get('session_id')
    
    # If no session ID is provided but a file was processed
    if not session_id and data.get('hasFile') and data.get('fileProcessed'):
        # Use the first available session
        if sessions:
            session_id = list(sessions.keys())[0]
    
    # Check if session exists
    if session_id and session_id in sessions:
        response = get_bot_response(message, sessions[session_id])
    else:
        response = {
            'message': "Please upload a CSV file first so I can analyze it."
        }
    response['session_id'] = session_id  # ✅ attach the session_id again
    return jsonify(response)

@app.route('/visualizations/<session_id>/<filename>')
def get_visualization(session_id, filename):
    """Serve visualization images"""
    file_path = os.path.join(app.config['VISUALIZATIONS_DIR'], session_id, filename)
    if os.path.exists(file_path):
        return send_file(file_path, mimetype='image/png')
    else:
        return 'Visualization not found', 404

@app.route('/api/download/<session_id>/<plot_type>')
def download_plot(session_id, plot_type):
    """Download a plot image"""
    if session_id in sessions and sessions[session_id].get('visualizations'):
        visualizations = sessions[session_id]['visualizations']
        
        if plot_type == 'initial' and os.path.exists(visualizations['initial']):
            return send_file(
                visualizations['initial'],
                as_attachment=True,
                download_name=f"nimbus_initial_plot_{session_id}.png",
                mimetype='image/png'
            )
        elif plot_type == 'salted' and os.path.exists(visualizations['salted']):
            return send_file(
                visualizations['salted'],
                as_attachment=True,
                download_name=f"nimbus_salted_plot_{session_id}.png",
                mimetype='image/png'
            )
        elif plot_type == 'smoothed' and os.path.exists(visualizations['smoothed']):
            return send_file(
                visualizations['smoothed'],
                as_attachment=True,
                download_name=f"nimbus_smoothed_plot_{session_id}.png",
                mimetype='image/png'
            )
        elif plot_type == 'final' and os.path.exists(visualizations['final']):
            return send_file(
                visualizations['final'],
                as_attachment=True,
                download_name=f"nimbus_final_plot_{session_id}.png",
                mimetype='image/png'
            )
    
    return 'Visualization not found', 404

@app.route('/api/download/<session_id>/processed_data')
def download_processed_data(session_id):
    """Download the processed data as CSV"""
    if session_id in sessions and sessions[session_id].get('processed_data') is not None:
        # Create a temporary file
        fd, temp_path = tempfile.mkstemp(suffix='.csv')
        
        try:
            # Write the processed data to the temp file
            sessions[session_id]['processed_data'].to_csv(temp_path, index=False)
            
            return send_file(
                temp_path,
                as_attachment=True,
                download_name=f"nimbus_processed_data_{session_id}.csv",
                mimetype='text/csv'
            )
        finally:
            # Clean up the temp file (will be deleted after the response is sent)
            os.close(fd)
    else:
        return 'Processed data not found', 404

if __name__ == '__main__':
    app.run(debug=True)