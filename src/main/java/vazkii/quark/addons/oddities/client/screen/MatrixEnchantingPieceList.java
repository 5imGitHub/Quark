package vazkii.quark.addons.oddities.client.screen;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.Mth;
import vazkii.quark.addons.oddities.container.EnchantmentMatrix.Piece;

public class MatrixEnchantingPieceList extends ObjectSelectionList<MatrixEnchantingPieceList.PieceEntry> {

	private final MatrixEnchantingScreen parent;
	private final int listWidth;

	public MatrixEnchantingPieceList(MatrixEnchantingScreen parent, int listWidth, int listHeight, int top, int bottom, int entryHeight) {
		super(parent.getMinecraft(), listWidth, listHeight, top, bottom, entryHeight);
		this.listWidth = listWidth;
		this.parent = parent;
	}

	@Override
	protected int getScrollbarPosition() {
		return getLeft() + this.listWidth - 5;
	}

	@Override
	public int getRowWidth() {
		return this.listWidth;
	}

	public void refresh() {
		clearEntries();

		if(parent.listPieces != null)
			for(int i : parent.listPieces) {
				Piece piece = parent.getPiece(i);
				if(piece != null)
					addEntry(new PieceEntry(piece, i));
			}
	}

	@Override
	public void render(PoseStack stack, int p_render_1_, int p_render_2_, float p_render_3_) {
		int i = this.getScrollbarPosition();
		int j = i + 6;
		int k = this.getRowLeft();
		int l = this.y0 + 4 - (int)this.getScrollAmount();
		
		fill(stack, getLeft(), getTop(), getLeft() + getWidth() + 1, getTop() + getHeight(), 0xFF2B2B2B);
		
		Window main = parent.getMinecraft().getWindow();
		int res = (int) main.getGuiScale();
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor(getLeft() * res, (main.getGuiScaledHeight() - getBottom()) * res, getWidth() * res, getHeight() * res);
		renderList(stack, k, l, p_render_1_, p_render_2_, p_render_3_);
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
		
		renderScroll(i, j);
	}

	protected int getMaxScroll2() {
		return Math.max(0, this.getMaxPosition() - (this.y1 - this.y0 - 4));
	}

	private void renderScroll(int i, int j) {
		int j1 = this.getMaxScroll2();
		if (j1 > 0) {
			int k1 = (int)((float)((this.y1 - this.y0) * (this.y1 - this.y0)) / (float)this.getMaxPosition());
			k1 = Mth.clamp(k1, 32, this.y1 - this.y0 - 8);
			int l1 = (int)this.getScrollAmount() * (this.y1 - this.y0 - k1) / j1 + this.y0;
			if (l1 < this.y0) {
				l1 = this.y0;
			}
			
			RenderSystem.disableTexture();
			Tesselator tessellator = Tesselator.getInstance();
			BufferBuilder bufferbuilder = tessellator.getBuilder();
			bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			bufferbuilder.vertex((double)i, (double)this.y1, 0.0D).uv(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
			bufferbuilder.vertex((double)j, (double)this.y1, 0.0D).uv(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
			bufferbuilder.vertex((double)j, (double)this.y0, 0.0D).uv(1.0F, 0.0F).color(0, 0, 0, 255).endVertex();
			bufferbuilder.vertex((double)i, (double)this.y0, 0.0D).uv(0.0F, 0.0F).color(0, 0, 0, 255).endVertex();
			tessellator.end();
			
			bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			bufferbuilder.vertex((double)i, (double)(l1 + k1), 0.0D).uv(0.0F, 1.0F).color(128, 128, 128, 255).endVertex();
			bufferbuilder.vertex((double)j, (double)(l1 + k1), 0.0D).uv(1.0F, 1.0F).color(128, 128, 128, 255).endVertex();
			bufferbuilder.vertex((double)j, (double)l1, 0.0D).uv(1.0F, 0.0F).color(128, 128, 128, 255).endVertex();
			bufferbuilder.vertex((double)i, (double)l1, 0.0D).uv(0.0F, 0.0F).color(128, 128, 128, 255).endVertex();
			tessellator.end();
			// TODO fixme
			bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			bufferbuilder.vertex((double)i, (double)(l1 + k1 - 1), 0.0D).uv(0.0F, 1.0F).color(192, 192, 192, 255).endVertex();
			bufferbuilder.vertex((double)(j - 1), (double)(l1 + k1 - 1), 0.0D).uv(1.0F, 1.0F).color(192, 192, 192, 255).endVertex();
			bufferbuilder.vertex((double)(j - 1), (double)l1, 0.0D).uv(1.0F, 0.0F).color(192, 192, 192, 255).endVertex();
			bufferbuilder.vertex((double)i, (double)l1, 0.0D).uv(0.0F, 0.0F).color(192, 192, 192, 255).endVertex();
			tessellator.end();
			RenderSystem.enableTexture();
		}
	}

	@Override
	protected void renderBackground(PoseStack stack) {
		// NO-OP
	}

	protected class PieceEntry extends ObjectSelectionList.Entry<PieceEntry> {

		final Piece piece;
		final int index;

		PieceEntry(Piece piece, int index) {
			this.piece = piece;
			this.index = index;
		}

		@Override
		public void render(PoseStack stack, int entryIdx, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hover, float partialTicks) {
			if(hover)
				parent.hoveredPiece = piece;

			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.setShaderTexture(0, MatrixEnchantingScreen.BACKGROUND);
			
			stack.pushPose();
			stack.translate(left + (listWidth - 7) / 2f, top + entryHeight / 2f, 0);
			stack.scale(0.5F, 0.5F, 0.5F);
			stack.translate(-8, -8, 0);
			parent.renderPiece(stack, piece, 1F);
			stack.popPose();
		}

		@Override
		public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
			parent.selectedPiece = index;
			setSelected(this);
			return false;
		}

		@Override
		public Component getNarration() {
			return new TextComponent("");
		}

	}

}