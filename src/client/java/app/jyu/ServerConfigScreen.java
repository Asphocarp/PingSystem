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
        // If the book selection popup is visible, handle its interactions first.
        if (this.showBookSelection && button == 0 && this.availableBooks != null) {
            int popupX = this.bookSelectionButton.getX();
            int popupY = this.bookSelectionButton.getY() + this.bookSelectionButton.getHeight() + 2;
            int popupWidth = this.bookSelectionButton.getWidth();

            List<Map.Entry<Integer, String>> sortedBooks = new ArrayList<>(this.availableBooks.entrySet());
            sortedBooks.sort(Map.Entry.comparingByKey());
            
            int displayItemCount = 0;
            int textHeight = this.textRenderer.fontHeight;
            int itemHeightWithPadding = textHeight + 5;
            
            // Calculate actual number of items that would be rendered up to maxDisplayItems (10)
            for (int i = 0; i < sortedBooks.size() && i < 10; i++) {
                displayItemCount++;
            }
            
            int popupContentHeight = displayItemCount * itemHeightWithPadding;
            int totalPopupHeight = popupContentHeight + 10; // 5px top/bottom padding for the list box

            // Check if the click is within the bounds of the rendered popup list
            boolean clickInsidePopupRenderedArea = mouseX >= popupX && mouseX < (popupX + popupWidth) &&
                                                  mouseY >= popupY && mouseY < (popupY + totalPopupHeight);

            if (clickInsidePopupRenderedArea) {
                // Click is inside the popup's visual area. Check if it's on an item.
                int currentItemY = popupY + 5; // Start Y for the first item content
                int itemsProcessed = 0;
                for (Map.Entry<Integer, String> entry : sortedBooks) {
                    if (itemsProcessed >= displayItemCount) break; // Only check visible items

                    // Check if the click is within the Y bounds of the current item
                    if (mouseY >= currentItemY && mouseY < (currentItemY + textHeight + 2)) { // +2 for a bit of leeway
                        // Click is on this book item
                        this.selectedBookId = entry.getKey();
                        this.showBookSelection = false; // Close popup after selection
                        this.updateButtonTexts();
                        return true; // Event handled by selecting a book
                    }
                    currentItemY += itemHeightWithPadding;
                    itemsProcessed++;
                }
                // Click was inside the popup's rendered area but not on any specific item (e.g., on padding).
                // Consume the click to prevent interaction with elements underneath the popup and keep it open.
                return true; 
            } else {
                // Click was outside the rendered popup area. Close the popup.
                this.showBookSelection = false;
                // The click was not handled by the popup list itself, so it might be for another widget.
                // Let super.mouseClicked handle it by falling through.
            }
        }

        // If the click was not handled by the popup logic above, or if the popup was not shown,
        // delegate to the default screen click handling which includes all child widgets like TextFields and Buttons.
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