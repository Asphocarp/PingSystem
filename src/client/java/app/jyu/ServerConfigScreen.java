package app.jyu;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.Map;

import static app.jyu.NetworkingConstants.UPDATE_CONFIG_PACKET;

public class ServerConfigScreen extends Screen {
    private final Screen parent;
    
    // Current config values (received from server)
    private int currentBookId = 1;
    private int highlightColor = 0xFFEB9D39;
    private boolean quizEnabled = true;
    private int quizTimeoutSeconds = 30;
    private boolean autoPingEnabled = true;
    
    // Available books (received from server)
    private Map<Integer, String> availableBooks;
    
    // GUI components
    private ButtonWidget bookSelectionButton;
    private TextFieldWidget colorField;
    private CheckboxWidget quizEnabledCheckbox;
    private TextFieldWidget timeoutField;
    private CheckboxWidget autoPingCheckbox;
    private ButtonWidget saveButton;
    private ButtonWidget cancelButton;
    
    // Book selection popup state
    private boolean showBookSelection = false;
    private int selectedBookId;
    
    public ServerConfigScreen(Screen parent) {
        super(Text.literal("PingSystem Server Configuration"));
        this.parent = parent;
        this.selectedBookId = currentBookId;
    }
    
    public void updateConfig(int bookId, int color, boolean quiz, int timeout, boolean autoPing, Map<Integer, String> books) {
        this.currentBookId = bookId;
        this.highlightColor = color;
        this.quizEnabled = quiz;
        this.quizTimeoutSeconds = timeout;
        this.autoPingEnabled = autoPing;
        this.availableBooks = books;
        this.selectedBookId = bookId;
        
        // Update GUI components if they exist
        if (bookSelectionButton != null) {
            updateButtonTexts();
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int startY = 70;
        int currentY = startY;
        int spacing = 35; // More consistent spacing
        int buttonWidth = 250; // Slightly wider for better appearance
        int buttonHeight = 20;
        
        // Book selection button
        String bookName = availableBooks != null ? 
            availableBooks.getOrDefault(selectedBookId, "Book " + selectedBookId) : 
            "Book " + selectedBookId;
        bookSelectionButton = ButtonWidget.builder(
            Text.literal("Dictionary: " + bookName),
            button -> toggleBookSelection()
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build();
        this.addDrawableChild(bookSelectionButton);
        
        // Color field (with space for label above)
        currentY += spacing + 15; // Extra space for label
        colorField = new TextFieldWidget(this.textRenderer, centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight, Text.literal("Color"));
        colorField.setText("0x" + Integer.toHexString(highlightColor).toUpperCase());
        this.addDrawableChild(colorField);
        
        // Quiz enabled checkbox
        currentY += spacing;
        quizEnabledCheckbox = new CheckboxWidget(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight, Text.literal("Quiz System Enabled"), quizEnabled);
        this.addDrawableChild(quizEnabledCheckbox);
        
        // Timeout field (with space for label above)
        currentY += spacing + 15; // Extra space for label
        timeoutField = new TextFieldWidget(this.textRenderer, centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight, Text.literal("Timeout"));
        timeoutField.setText(String.valueOf(quizTimeoutSeconds));
        this.addDrawableChild(timeoutField);
        
        // Auto ping checkbox
        currentY += spacing;
        autoPingCheckbox = new CheckboxWidget(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight, Text.literal("Auto-Ping on Actions"), autoPingEnabled);
        this.addDrawableChild(autoPingCheckbox);
        
        // Save and Cancel buttons with more space above
        currentY += spacing + 15;
        saveButton = ButtonWidget.builder(
            Text.literal("Save").formatted(Formatting.GREEN),
            button -> saveConfig()
        ).dimensions(centerX - 105, currentY, 100, buttonHeight).build();
        this.addDrawableChild(saveButton);
        
        cancelButton = ButtonWidget.builder(
            Text.literal("Cancel").formatted(Formatting.RED),
            button -> this.close()
        ).dimensions(centerX + 5, currentY, 100, buttonHeight).build();
        this.addDrawableChild(cancelButton);
    }
    
    private void toggleBookSelection() {
        showBookSelection = !showBookSelection;
    }
    
    private void updateButtonTexts() {
        if (bookSelectionButton != null && availableBooks != null) {
            String bookName = availableBooks.getOrDefault(selectedBookId, "Book " + selectedBookId);
            bookSelectionButton.setMessage(Text.literal("Dictionary: " + bookName));
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        
        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Permission warning for non-OPs
        context.drawCenteredTextWithShadow(this.textRenderer, 
            Text.literal("Only OPs can modify server configuration").formatted(Formatting.YELLOW), 
            this.width / 2, 40, 0xFFFF55);
        
        super.render(context, mouseX, mouseY, delta);
        
        // Book selection popup
        if (showBookSelection && availableBooks != null) {
            renderBookSelectionPopup(context, mouseX, mouseY);
        }
        
        // Labels for text fields - properly aligned with their input fields
        int centerX = this.width / 2;
        int labelX = centerX - 125; // Align labels to the left of input fields
        
        // Color field label
        if (colorField != null) {
            context.drawTextWithShadow(this.textRenderer, "Highlight Color (0xAARRGGBB):", 
                labelX, colorField.getY() - 12, 0xFFFFFF);
        }
        
        // Timeout field label  
        if (timeoutField != null) {
            context.drawTextWithShadow(this.textRenderer, "Quiz Timeout (seconds):", 
                labelX, timeoutField.getY() - 12, 0xFFFFFF);
        }
    }
    
    private void renderBookSelectionPopup(DrawContext context, int mouseX, int mouseY) {
        int popupX = this.width / 2 - 150;
        int popupY = 80;
        int popupWidth = 300;
        int maxBooks = Math.min(10, availableBooks.size());
        int popupHeight = maxBooks * 20 + 20;
        
        // Background
        context.fill(popupX - 5, popupY - 5, popupX + popupWidth + 5, popupY + popupHeight + 5, 0xFF000000);
        context.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xFF333333);
        
        // Header
        context.drawTextWithShadow(this.textRenderer, "Select Dictionary:", popupX + 5, popupY + 5, 0xFFFFFF);
        
        // Book list
        int y = popupY + 20;
        for (Map.Entry<Integer, String> entry : availableBooks.entrySet()) {
            int bookId = entry.getKey();
            String bookName = entry.getValue();
            
            boolean isHovered = mouseX >= popupX && mouseX <= popupX + popupWidth && 
                               mouseY >= y && mouseY <= y + 15;
            boolean isSelected = bookId == selectedBookId;
            
            int color = isSelected ? 0xFF55FF55 : (isHovered ? 0xFF555555 : 0xFFFFFFFF);
            context.drawTextWithShadow(this.textRenderer, 
                String.format("%d. %s", bookId, bookName), 
                popupX + 10, y, color);
            
            y += 20;
            if (y > popupY + popupHeight - 20) break; // Prevent overflow
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showBookSelection && availableBooks != null) {
            int popupX = this.width / 2 - 150;
            int popupY = 80;
            int popupWidth = 300;
            
            // Check if clicked inside popup
            if (mouseX >= popupX && mouseX <= popupX + popupWidth && mouseY >= popupY + 20) {
                int y = popupY + 20;
                for (Map.Entry<Integer, String> entry : availableBooks.entrySet()) {
                    if (mouseY >= y && mouseY <= y + 15) {
                        selectedBookId = entry.getKey();
                        showBookSelection = false;
                        updateButtonTexts();
                        return true;
                    }
                    y += 20;
                }
            } else {
                showBookSelection = false;
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void saveConfig() {
        try {
            // Parse color
            String colorText = colorField.getText().trim();
            if (colorText.startsWith("0x")) {
                colorText = colorText.substring(2);
            }
            int color = (int) Long.parseLong(colorText, 16);
            
            // Parse timeout
            int timeout = Integer.parseInt(timeoutField.getText().trim());
            
            // Create update packet
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(selectedBookId);
            buf.writeInt(color);
            buf.writeBoolean(quizEnabledCheckbox.isChecked());
            buf.writeInt(timeout);
            buf.writeBoolean(autoPingCheckbox.isChecked());
            
            // Send to server
            ClientPlayNetworking.send(UPDATE_CONFIG_PACKET, buf);
            
            // Show success message and close
            MinecraftClient.getInstance().player.sendMessage(
                Text.literal("Server configuration updated!").formatted(Formatting.GREEN), false);
            this.close();
            
        } catch (NumberFormatException e) {
            MinecraftClient.getInstance().player.sendMessage(
                Text.literal("Invalid number format in fields!").formatted(Formatting.RED), false);
        } catch (Exception e) {
            MinecraftClient.getInstance().player.sendMessage(
                Text.literal("Failed to save configuration: " + e.getMessage()).formatted(Formatting.RED), false);
        }
    }
    
    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
} 