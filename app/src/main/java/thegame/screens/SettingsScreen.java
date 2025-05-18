package thegame.screens;

import static org.lwjgl.opengl.GL11.*;

import thegame.App;
import thegame.Screen;
import thegame.onScreenObjects.Button;
import thegame.onScreenObjects.Slider;
import thegame.utils.FontRenderer;

public class SettingsScreen implements Screen {
    private final App app;
    private FontRenderer fontRenderer;
    
    // Settings values
    private float musicVolume = 0.7f;
    private float effectsVolume = 0.8f;
    private boolean fullscreen = false;
    private boolean vsync = true;
    
    // UI elements
    private Slider musicSlider;
    private Slider effectsSlider;
    private Button fullscreenToggle;
    private Button vsyncToggle;
    private Button applyButton;
    private Button backButton;
    
    private double currentMouseX = 0, currentMouseY = 0;

    public SettingsScreen(App app) {
        this.app = app;
        
        // Initialize font renderer
        fontRenderer = new FontRenderer();
        fontRenderer.loadFont("F:/temp/theGame/untitledGame/app/src/main/resources/fonts/pf_tempesta_seven_bold.ttf");
        
        // Create sliders
        int sliderWidth = 300;
        int sliderHeight = 30;
        int centerX = App.WINDOW_WIDTH / 2 - sliderWidth / 2;
        
        musicSlider = new Slider(centerX, 200, sliderWidth, sliderHeight, musicVolume);
        effectsSlider = new Slider(centerX, 270, sliderWidth, sliderHeight, effectsVolume);
        
        // Create toggle buttons
        int toggleWidth = 150;
        int toggleHeight = 40;
        
        fullscreenToggle = new Button(centerX, 340, toggleWidth, toggleHeight, 
                                    fullscreen ? 0.2f : 0.6f, 0.4f, 0.4f, 
                                    fullscreen ? "Fullscreen: ON" : "Fullscreen: OFF");
        
        vsyncToggle = new Button(centerX + toggleWidth + 20, 340, toggleWidth, toggleHeight,
                                vsync ? 0.2f : 0.6f, 0.4f, 0.4f,
                                vsync ? "V-Sync: ON" : "V-Sync: OFF");
        
        // Create action buttons
        int buttonWidth = 150;
        int buttonHeight = 40;
        
        applyButton = new Button(centerX - buttonWidth - 10, 410, buttonWidth, buttonHeight, 
                                0.2f, 0.6f, 0.2f, "Apply");
        backButton = new Button(centerX + 10, 410, buttonWidth, buttonHeight, 
                              0.6f, 0.2f, 0.2f, "Back");
    }

    @Override
    public void render() {
        glClearColor(0.1f, 0.1f, 0.2f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);
        
        // Draw title
        fontRenderer.renderCenteredText("SETTINGS", App.WINDOW_WIDTH / 2, 100, 2.0f);
        
        // Draw slider labels
        fontRenderer.renderText("Music Volume:", musicSlider.getX(), musicSlider.getY() - 30, 1.0f);
        fontRenderer.renderText("Effects Volume:", effectsSlider.getX(), effectsSlider.getY() - 30, 1.0f);
        
        // Display current values
        fontRenderer.renderText(String.format("%.0f%%", musicVolume * 100), 
                              musicSlider.getX() + musicSlider.getWidth() + 20, 
                              musicSlider.getY(), 1.0f);
        fontRenderer.renderText(String.format("%.0f%%", effectsVolume * 100), 
                              effectsSlider.getX() + effectsSlider.getWidth() + 20, 
                              effectsSlider.getY(), 1.0f);
        
        // Draw UI elements
        musicSlider.render((float)currentMouseX, (float)currentMouseY);
        effectsSlider.render((float)currentMouseX, (float)currentMouseY);
        fullscreenToggle.render((float)currentMouseX, (float)currentMouseY);
        vsyncToggle.render((float)currentMouseX, (float)currentMouseY);
        applyButton.render((float)currentMouseX, (float)currentMouseY);
        backButton.render((float)currentMouseX, (float)currentMouseY);
    }

    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        float mx = (float)mouseX;
        float my = (float)mouseY;
        
        if (backButton.handleMouseClick(mx, my)) {
            app.setCurrentScreen(new TitleScreen(app));
            return;
        }
        
        if (applyButton.handleMouseClick(mx, my)) {
            // Save settings
            // In a real app, you would persist these settings
            // For this demo, just go back to title screen
            app.setCurrentScreen(new TitleScreen(app));
            return;
        }
        
        if (fullscreenToggle.handleMouseClick(mx, my)) {
            fullscreen = !fullscreen;
            fullscreenToggle.setCaption(fullscreen ? "Fullscreen: ON" : "Fullscreen: OFF");
            fullscreenToggle.setColors(fullscreen ? 0.2f : 0.6f, 0.4f, 0.4f);
            return;
        }
        
        if (vsyncToggle.handleMouseClick(mx, my)) {
            vsync = !vsync;
            vsyncToggle.setCaption(vsync ? "V-Sync: ON" : "V-Sync: OFF");
            vsyncToggle.setColors(vsync ? 0.2f : 0.6f, 0.4f, 0.4f);
            return;
        }
        
        // Handle slider interactions
        if (musicSlider.handleMouseClick(mx, my)) {
            musicVolume = musicSlider.getValue();
            return;
        }
        
        if (effectsSlider.handleMouseClick(mx, my)) {
            effectsVolume = effectsSlider.getValue();
            return;
        }
    }

    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        this.currentMouseX = mouseX;
        this.currentMouseY = mouseY;
        
        // Update sliders if being dragged
        if (musicSlider.isDragging()) {
            musicSlider.handleMouseMove(mouseX, mouseY);
            musicVolume = musicSlider.getValue();
        }
        
        if (effectsSlider.isDragging()) {
            effectsSlider.handleMouseMove(mouseX, mouseY);
            effectsVolume = effectsSlider.getValue();
        }
    }

    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {
        musicSlider.handleMouseRelease(mouseX, mouseY);
        effectsSlider.handleMouseRelease(mouseX, mouseY);
    }
    
    @Override
    public void handleKeyPress(int key, int action) {
        // Not needed for this screen
    }
    
    @Override
    public void handleCharInput(int codepoint) {
        // Not needed for this screen
    }
}
