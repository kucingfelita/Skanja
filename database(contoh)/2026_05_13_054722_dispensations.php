<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        Schema::create('dispensations', function (Blueprint $table) {    $table->id();
    $table->foreignId('user_id')->constrained('users');
    $table->text('reason');
    $table->string('file_path'); // Path surat STP2K
    $table->enum('status', ['pending', 'approved', 'rejected'])->default('pending');
    $table->softDeletes();
    $table->timestamps();
});
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('dispensations');
    }
};
