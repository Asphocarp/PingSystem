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
    
    // State for scrollable book list
    private int bookScrollOffset = 0;
    private final int maxDisplayBooksInPopup = 15; // Max items to show at once
    
    public ServerConfigScreen(Screen parent) {
        super(Text.literal("QuizCraft Server Configuration"));
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
        if (showBookSelection) {
            bookScrollOffset = 0; // Reset scroll on open
        }
    }
    
    private void updateButtonTexts() {
        if (bookSelectionButton != null && availableBooks != null) {
            String bookName = availableBooks.getOrDefault(selectedBookId, "Book " + selectedBookId);
            bookSelectionButton.setMessage(Text.literal("Dictionary: " + bookName));
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Always render the background
        this.renderBackground(context);
        
        // Always render the main screen title and warning message
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, 
            Text.literal("Only OPs can modify server configuration").formatted(Formatting.YELLOW), 
            this.width / 2, 40, 0xFFFF55);
        
        if (showBookSelection && availableBooks != null) {
            // When dropdown is active, only render the book button (needed as reference point)
            if (bookSelectionButton != null) {
                bookSelectionButton.render(context, mouseX, mouseY, delta);
            }
            
            // Render the dropdown on top of everything
            renderBookSelectionPopup(context, mouseX, mouseY);
        } else {
            // When dropdown is not active, render everything normally
            super.render(context, mouseX, mouseY, delta); // Renders all addDrawableChild items
            
            // Add custom labels for text fields
            int centerX = this.width / 2;
            int labelX = centerX - (250/2); 
            
            if (colorField != null) {
                context.drawTextWithShadow(this.textRenderer, "Highlight Color (0xAARRGGBB):", 
                    labelX, colorField.getY() - 12, 0xFFFFFF);
            }
            if (timeoutField != null) {
                context.drawTextWithShadow(this.textRenderer, "Quiz Timeout (seconds):", 
                    labelX, timeoutField.getY() - 12, 0xFFFFFF);
            }
        }
    }
    
    private void renderBookSelectionPopup(DrawContext context, int mouseX, int mouseY) {
        if (availableBooks == null || availableBooks.isEmpty()) {
            showBookSelection = false; 
            return;
        }

        int popupX = bookSelectionButton.getX();
        int popupY = bookSelectionButton.getY() + bookSelectionButton.getHeight() + 2;
        int popupWidth = bookSelectionButton.getWidth();

        List<Map.Entry<Integer, String>> sortedBooks = new ArrayList<>(availableBooks.entrySet());
        sortedBooks.sort(Map.Entry.comparingByKey());

        int startIdx = bookScrollOffset;
        int endIdx = Math.min(bookScrollOffset + maxDisplayBooksInPopup, sortedBooks.size());
        List<Map.Entry<Integer, String>> booksToDisplay = sortedBooks.subList(startIdx, endIdx);

        int textHeight = this.textRenderer.fontHeight;
        int itemHeight = textHeight + 5; 

        int listRenderHeight = Math.min(maxDisplayBooksInPopup, sortedBooks.size()) * itemHeight;
        if (sortedBooks.size() == 0) listRenderHeight = itemHeight; 

        boolean needsScrolling = availableBooks.size() > maxDisplayBooksInPopup;
        int scrollButtonsAreaHeight = needsScrolling ? (textHeight + 4 + 2) : 0; 
        
        int popupInternalContentHeight = listRenderHeight + scrollButtonsAreaHeight;
        int popupDrawnHeight = popupInternalContentHeight + 10; 

        // Solid background for better readability
        context.fill(popupX - 1, popupY - 1, popupX + popupWidth + 1, popupY + popupDrawnHeight + 1, 0xFF000000); // Border
        context.fill(popupX, popupY, popupX + popupWidth, popupY + popupDrawnHeight, 0xFF2A2A2A); // Solid dark gray background (opaque)

        int itemRenderY = popupY + 5;
        if (booksToDisplay.isEmpty() && sortedBooks.isEmpty()){
             context.drawTextWithShadow(this.textRenderer, "No books available.", popupX + 5, itemRenderY, 0xFFAAAAAA);
        } else {
            for (Map.Entry<Integer, String> entry : booksToDisplay) {
                String bookName = String.format("%d: %s", entry.getKey(), entry.getValue());
                boolean isHovered = mouseX >= popupX && mouseX <= popupX + popupWidth &&
                                mouseY >= itemRenderY -1 && mouseY <= itemRenderY + textHeight +1; 
                boolean isSelected = entry.getKey() == selectedBookId;
                int textColor = isSelected ? 0xFF88FF88 : (isHovered ? 0xFFDDDDDD : 0xFFFFFFFF); 
                context.drawTextWithShadow(this.textRenderer, bookName, popupX + 5, itemRenderY, textColor);
                itemRenderY += itemHeight;
            }
        }

        if (needsScrolling) {
            int scrollIndicatorRenderY = popupY + listRenderHeight + 5 + 2; 
            
            boolean canScrollUp = bookScrollOffset > 0;
            String upArrow = "^";
            int upArrowWidth = this.textRenderer.getWidth(upArrow);
            int upArrowX = popupX + (popupWidth / 2) - upArrowWidth - 5;
            context.drawTextWithShadow(this.textRenderer, upArrow, upArrowX, scrollIndicatorRenderY, canScrollUp ? 0xFFFFFFFF : 0xFF808080);

            boolean canScrollDown = bookScrollOffset < (sortedBooks.size() - maxDisplayBooksInPopup);
            String downArrow = "v";
            int downArrowX = popupX + (popupWidth / 2) + 5;
            context.drawTextWithShadow(this.textRenderer, downArrow, downArrowX, scrollIndicatorRenderY, canScrollDown ? 0xFFFFFFFF : 0xFF808080);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.showBookSelection && button == 0 && this.availableBooks != null && !this.availableBooks.isEmpty()) {
            int popupX = this.bookSelectionButton.getX();
            int popupY = this.bookSelectionButton.getY() + this.bookSelectionButton.getHeight() + 2;
            int popupWidth = this.bookSelectionButton.getWidth();

            List<Map.Entry<Integer, String>> sortedBooks = new ArrayList<>(this.availableBooks.entrySet());
            sortedBooks.sort(Map.Entry.comparingByKey());

            int textHeight = this.textRenderer.fontHeight;
            int itemHeight = textHeight + 5;

            int listRenderHeight = Math.min(maxDisplayBooksInPopup, sortedBooks.size()) * itemHeight;
            boolean needsScrolling = availableBooks.size() > maxDisplayBooksInPopup;
            int scrollButtonsAreaHeight = needsScrolling ? (textHeight + 4 + 2) : 0; 
            int popupInternalContentHeight = listRenderHeight + scrollButtonsAreaHeight;
            int popupDrawnHeight = popupInternalContentHeight + 10; 

            boolean clickInsidePopupShell = mouseX >= popupX && mouseX < (popupX + popupWidth) &&
                                            mouseY >= popupY && mouseY < (popupY + popupDrawnHeight);

            if (clickInsidePopupShell) {
                // Check click on scroll indicators first
                if (needsScrolling) {
                    int scrollIndicatorClickY = popupY + listRenderHeight + 5 + 2; 
                    int scrollIndicatorHeight = textHeight + 4; 

                    int upArrowX = popupX + (popupWidth / 2) - this.textRenderer.getWidth("^") - 5;
                    int upArrowClickWidth = this.textRenderer.getWidth("^") + 10; 
                    if (mouseX >= upArrowX - 5 && mouseX <= upArrowX + upArrowClickWidth - 5 && 
                        mouseY >= scrollIndicatorClickY && mouseY <= scrollIndicatorClickY + scrollIndicatorHeight) {
                        if (bookScrollOffset > 0) {
                            bookScrollOffset--;
                            return true; 
                        }
                    }

                    int downArrowX = popupX + (popupWidth / 2) + 5;
                    int downArrowClickWidth = this.textRenderer.getWidth("v") + 10; 
                    if (mouseX >= downArrowX - 5 && mouseX <= downArrowX + downArrowClickWidth -5 && 
                        mouseY >= scrollIndicatorClickY && mouseY <= scrollIndicatorClickY + scrollIndicatorHeight) {
                        if (bookScrollOffset < (sortedBooks.size() - maxDisplayBooksInPopup)) {
                            bookScrollOffset++;
                            return true; 
                        }
                    }
                }

                // Check click on book items
                int itemRenderY = popupY + 5;
                List<Map.Entry<Integer, String>> booksToDisplay = sortedBooks.subList(bookScrollOffset, Math.min(bookScrollOffset + maxDisplayBooksInPopup, sortedBooks.size()));
                for (Map.Entry<Integer, String> entry : booksToDisplay) {
                    if (mouseX >= popupX && mouseX <= popupX + popupWidth &&
                        mouseY >= itemRenderY -1 && mouseY <= itemRenderY + textHeight +1 ) { 
                        this.selectedBookId = entry.getKey();
                        this.showBookSelection = false;
                        this.updateButtonTexts();
                        return true; 
                    }
                    itemRenderY += itemHeight;
                }
                return true; // Clicked on popup background, consume to keep it open
            } else {
                this.showBookSelection = false; // Clicked outside popup, close it
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