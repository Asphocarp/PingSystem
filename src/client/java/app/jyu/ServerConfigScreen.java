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
import java.util.ArrayList;
import java.util.List;

import static app.jyu.NetworkingConstants.UPDATE_CONFIG_PACKET;

public class ServerConfigScreen extends Screen {
    private final Screen parent;
    
    // Current config values (received from server)
    private int currentBookId = 1;
    private int highlightColor = 0xFFEB9D39;
    private boolean quizEnabled = true;
    private int quizTimeoutSeconds = 30;
    
    // Available books (received from server)
    private Map<Integer, String> availableBooks;
    
    // GUI components
    private ButtonWidget bookSelectionButton;
    private TextFieldWidget colorField;
    private CheckboxWidget quizEnabledCheckbox;
    private TextFieldWidget timeoutField;
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
    
    public void updateConfig(int bookId, int color, boolean quiz, int timeout, Map<Integer, String> books) {
        this.currentBookId = bookId;
        this.highlightColor = color;
        this.quizEnabled = quiz;
        this.quizTimeoutSeconds = timeout;
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
        int spacing = 35;
        int buttonWidth = 250;
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
        currentY += spacing + 15;
        colorField = new TextFieldWidget(this.textRenderer, centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight, Text.literal("Color"));
        colorField.setText("0x" + Integer.toHexString(highlightColor).toUpperCase());
        this.addDrawableChild(colorField);
        
        // Quiz enabled checkbox
        currentY += spacing;
        quizEnabledCheckbox = new CheckboxWidget(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight, Text.literal("Quiz System Enabled"), quizEnabled);
        this.addDrawableChild(quizEnabledCheckbox);
        
        // Timeout field (with space for label above)
        currentY += spacing + 15;
        timeoutField = new TextFieldWidget(this.textRenderer, centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight, Text.literal("Timeout"));
        timeoutField.setText(String.valueOf(quizTimeoutSeconds));
        this.addDrawableChild(timeoutField);
        
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
        int labelX = centerX - (250/2); // Align labels to the left of input fields, same as button start
        
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
        int popupX = bookSelectionButton.getX();
        int popupY = bookSelectionButton.getY() + bookSelectionButton.getHeight() + 2;
        int popupWidth = bookSelectionButton.getWidth();
        
        List<Map.Entry<Integer, String>> sortedBooks = new ArrayList<>(availableBooks.entrySet());
        sortedBooks.sort(Map.Entry.comparingByKey());

        int maxDisplayItems = 10;
        int displayItemCount = Math.min(sortedBooks.size(), maxDisplayItems);
        int popupHeight = (displayItemCount * (this.textRenderer.fontHeight + 5)) + 10; // Adjusted for padding

        // Background
        context.fill(popupX - 1, popupY - 1, popupX + popupWidth + 1, popupY + popupHeight + 1, 0xFF000000); // Border
        context.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xCC333333); // Semi-transparent background
        
        int itemY = popupY + 5;
        for (Map.Entry<Integer, String> entry : sortedBooks) {
            int bookId = entry.getKey();
            String bookName = String.format("%d: %s", bookId, entry.getValue());
            
            boolean isHovered = mouseX >= popupX && mouseX <= popupX + popupWidth && 
                               mouseY >= itemY && mouseY <= itemY + this.textRenderer.fontHeight + 2;
            boolean isSelected = bookId == selectedBookId;
            
            int textColor = isSelected ? 0xFF55FF55 : (isHovered ? 0xFFDDDDDD : 0xFFFFFFFF);
            context.drawTextWithShadow(this.textRenderer, 
                bookName, 
                popupX + 5, itemY + 1, textColor);
            
            itemY += this.textRenderer.fontHeight + 5;
            if (itemY >= popupY + popupHeight - 5) break; 
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && showBookSelection && availableBooks != null) {
            int popupX = bookSelectionButton.getX();
            int popupY = bookSelectionButton.getY() + bookSelectionButton.getHeight() + 2;
            int popupWidth = bookSelectionButton.getWidth();

            List<Map.Entry<Integer, String>> sortedBooks = new ArrayList<>(availableBooks.entrySet());
            sortedBooks.sort(Map.Entry.comparingByKey());
            
            int itemY = popupY + 5;
            for (Map.Entry<Integer, String> entry : sortedBooks) {
                if (mouseX >= popupX && mouseX <= popupX + popupWidth && 
                    mouseY >= itemY && mouseY <= itemY + this.textRenderer.fontHeight + 2) {
                    
                    selectedBookId = entry.getKey();
                    showBookSelection = false;
                    updateButtonTexts();
                    return true;
                }
                itemY += this.textRenderer.fontHeight + 5;
            }
            // If clicked outside the book list area but popup was shown, close it
            showBookSelection = false;
            return true; 
        }
        
        // Handle clicks on other elements like the book selection button itself
        if (bookSelectionButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (saveButton.mouseClicked(mouseX, mouseY, button)) {
             return true;
        }
        if (cancelButton.mouseClicked(mouseX, mouseY, button)) {
             return true;
        }
        if(quizEnabledCheckbox.mouseClicked(mouseX, mouseY, button)){
            return true;
        }
        // Let text fields handle their clicks
        if (colorField.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (timeoutField.mouseClicked(mouseX, mouseY, button)) {
            return true;
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
            
            // Send to server
            ClientPlayNetworking.send(UPDATE_CONFIG_PACKET, buf);
            
            // Show success message and close
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                    Text.literal("Server configuration updated!").formatted(Formatting.GREEN), false);
            }
            this.close();
            
        } catch (NumberFormatException e) {
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                    Text.literal("Invalid number format in fields!").formatted(Formatting.RED), false);
            }
        } catch (Exception e) {
             if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                    Text.literal("Failed to save configuration: " + e.getMessage()).formatted(Formatting.RED), false);
             }
        }
    }
    
    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
} 