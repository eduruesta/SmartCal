#!/bin/bash

echo "🚀 Starting Calendar Agent with MCP Support"
echo "=========================================="

# Check if MCP server is already running
if curl -s http://localhost:8080/health > /dev/null 2>&1; then
    echo "✅ MCP server is already running on port 8080"
else
    echo "🔧 Starting MCP server..."
    cd mcp-server
    python3 server.py &
    MCP_PID=$!
    echo "📝 MCP Server started with PID: $MCP_PID"
    cd ..
    
    # Wait for server to be ready
    echo "⏳ Waiting for MCP server to be ready..."
    for i in {1..10}; do
        if curl -s http://localhost:8080/health > /dev/null 2>&1; then
            echo "✅ MCP server is ready!"
            break
        fi
        sleep 1
        echo "   Attempt $i/10..."
    done
    
    if ! curl -s http://localhost:8080/health > /dev/null 2>&1; then
        echo "❌ MCP server failed to start"
        exit 1
    fi
fi

echo ""
echo "🎯 MCP Server Status:"
echo "   Health: $(curl -s http://localhost:8080/health)"
echo "   Tools available: $(curl -s http://localhost:8080/mcp/tools | jq '.tools | length')"

echo ""
echo "📱 Now you can:"
echo "   1. Run the Android app (will connect to localhost:8080 via 10.0.2.2:8080)"
echo "   2. Test MCP tools in the chat interface"
echo "   3. Try commands like:"
echo "      - 'What time is it?'"
echo "      - 'Add 3 days to 2024-12-25T10:00:00Z'"
echo "      - 'Format this date: 2024-12-25T10:30:00Z'"

echo ""
echo "🛑 To stop the MCP server later, run:"
echo "   pkill -f 'python3 server.py'"

echo ""
echo "🏗️  Architecture:"
echo "   Android App ←→ HTTP ←→ MCP Server ←→ Tools"
echo "   (Port 8080)"